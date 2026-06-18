package com.google.jetpackcamera.core.camera.mfs

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val TAG = "MfsCapturePipeline"

class MfsCapturePipeline(
    private val aligner: FrameAligner = FrameAligner(),
    private val merger: FrameMerger = FrameMerger(),
    private val distortionCorrector: LensDistortionCorrector? = null
) {
    private val cancelled = AtomicBoolean(false)

    fun cancel() {
        cancelled.set(true)
    }

    private fun checkCancelled() {
        if (cancelled.get()) {
            throw CancellationException("MFS capture cancelled by user")
        }
    }
    fun interface MfsProgressCallback {
        fun onProgress(stage: MfsStage, capturedFrames: Int, totalFrames: Int)
    }

    enum class MfsStage { CAPTURING, MERGING, SAVING, DONE }

    suspend fun captureAndMerge(
        imageCapture: ImageCapture,
        executor: java.util.concurrent.Executor,
        config: MfsConfig,
        progressCallback: MfsProgressCallback? = null
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            checkCancelled()
            val totalFrames = config.frameCount
            progressCallback?.onProgress(MfsStage.CAPTURING, 0, totalFrames)
            val frames = captureBurst(imageCapture, executor, config, progressCallback)
            if (frames.isEmpty()) {
                return@withContext Result.failure(
                    RuntimeException("MFS: no frames captured")
                )
            }
            progressCallback?.onProgress(MfsStage.MERGING, frames.size, totalFrames)

            if (frames.size == 1) {
                progressCallback?.onProgress(MfsStage.SAVING, 1, totalFrames)
                val corrected = distortionCorrector?.correct(frames.first().bitmap)
                    ?: frames.first().bitmap
                val look = config.lookProfile
                val contrastAmount = look?.contrast ?: 0.12f
                val saturationFactor = look?.saturation ?: 1.18f
                val contrasted = merger.adjustContrast(corrected, contrastAmount)
                val saturated = merger.boostSaturation(contrasted, saturationFactor)
                return@withContext Result.success(saturated)
            }

            val processingFrames = if (config.processingScale < 1f) {
                frames.map { frame ->
                    val newW = (frame.bitmap.width * config.processingScale).toInt()
                        .coerceAtLeast(64)
                    val newH = (frame.bitmap.height * config.processingScale).toInt()
                        .coerceAtLeast(64)
                    val scaled = Bitmap.createScaledBitmap(
                        frame.bitmap, newW, newH, true
                    )
                    frame.copy(bitmap = scaled)
                }
            } else {
                frames
            }

            val alignFrames = if (config.preFilterStrength > 0f) {
                processingFrames.map {
                    it.copy(bitmap = merger.preFilterFrame(it.bitmap))
                }
            } else {
                processingFrames
            }
            val reference = alignFrames.first()
            val targets = alignFrames.drop(1)

            val aligned = aligner.alignFrames(reference, targets)
            val allFrames = listOf(reference) + aligned
            val merged = merger.merge(allFrames, config.mergeStrategy)
            val look = config.lookProfile
            val denoiseStr = look?.noiseReduction ?: config.denoiseStrength
            val sharpenStr = look?.sharpness ?: config.sharpenStrength
            val enhanced = merger.enhanceDetail(merged, denoiseStr, sharpenStr)
            val corrected = distortionCorrector?.correct(enhanced) ?: enhanced
            val contrastAmount = look?.contrast ?: 0.12f
            val saturationFactor = look?.saturation ?: 1.18f
            val contrasted = merger.adjustContrast(corrected, contrastAmount)
            val saturated = merger.boostSaturation(contrasted, saturationFactor)

            progressCallback?.onProgress(MfsStage.SAVING, frames.size, totalFrames)

            val lookLabel = if (look != null) " lookProfile=active" else ""
            Log.d(
                TAG,
                "MFS done: ${frames.size}f " +
                    "strat=${config.mergeStrategy} " +
                    "preF=${config.preFilterStrength} " +
                    "denoise=${denoiseStr} " +
                    "sharpen=${sharpenStr}" +
                    lookLabel
            )
            Result.success(saturated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "MFS pipeline OOM", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "MFS pipeline failed", e)
            Result.failure(e)
        }
    }

    private suspend fun captureBurst(
        imageCapture: ImageCapture,
        executor: java.util.concurrent.Executor,
        config: MfsConfig,
        progressCallback: MfsProgressCallback? = null
    ): List<FrameData> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val totalFrames = config.frameCount
        val capturedCount = AtomicInteger(0)
        val frames = coroutineScope {
            val deferred = (0 until totalFrames).map { i ->
                async {
                    if (i > 0 && config.frameGapMs > 0L) {
                        kotlinx.coroutines.delay(config.frameGapMs)
                    }
                    checkCancelled()
                    val frame = captureSingleFrame(imageCapture, executor, i)
                    val completed = capturedCount.incrementAndGet()
                    progressCallback?.onProgress(MfsStage.CAPTURING, completed, totalFrames)
                    frame
                }
            }
            deferred.mapNotNull { it.await() }
        }
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Captured ${frames.size}/${totalFrames} frames in ${elapsed}ms")
        frames
    }

    private suspend fun captureSingleFrame(
        imageCapture: ImageCapture,
        executor: java.util.concurrent.Executor,
        index: Int
    ): FrameData? = suspendCancellableCoroutine { continuation ->
        imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bmp = imageProxyToBitmap(imageProxy)
                    imageProxy.close()
                    val frameData = bmp?.let {
                        FrameData(
                            bitmap = it,
                            index = index,
                            timestampMs = System.currentTimeMillis()
                        )
                    }
                    continuation.resume(frameData, null)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Frame $index failed", exception)
                    continuation.resume(null, null)
                }
            }
        )
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val decodeOptions = BitmapFactory.Options().apply {
                inScaled = false
                inDither = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            when (imageProxy.format) {
                ImageFormat.JPEG -> {
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                }

                ImageFormat.YUV_420_888 -> {
                    yuv420ToBitmap(imageProxy)
                }

                else -> {
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode ImageProxy", e)
            null
        }
    }

    private fun yuv420ToBitmap(imageProxy: ImageProxy): Bitmap? {
        try {
            val width = imageProxy.width
            val height = imageProxy.height
            val yPlane = imageProxy.planes[0]
            val uPlane = imageProxy.planes[1]
            val vPlane = imageProxy.planes[2]

            val yRowStride = yPlane.rowStride
            val uRowStride = uPlane.rowStride
            val vRowStride = vPlane.rowStride

            val yPixelStride = yPlane.pixelStride
            val uPixelStride = uPlane.pixelStride
            val vPixelStride = vPlane.pixelStride

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val pixels = IntArray(width * height)
            val halfWidth = (width + 1) / 2
            val halfHeight = (height + 1) / 2

            for (row in 0 until height) {
                val yRowOffset = row * yRowStride
                for (col in 0 until width) {
                    val yIdx = yRowOffset + col * yPixelStride
                    val yVal = yBuffer.get(yIdx).toInt() and 0xFF

                    val uvX = col / 2
                    val uvY = row / 2
                    val uIdx = uvY * uRowStride + uvX * uPixelStride
                    val vIdx = uvY * vRowStride + uvX * vPixelStride
                    val u = (uBuffer.get(uIdx).toInt() and 0xFF) - 128
                    val v = (vBuffer.get(vIdx).toInt() and 0xFF) - 128

                    val r = (yVal + (1436 * v) / 1024).coerceIn(0, 255)
                    val g = (yVal - (352 * u) / 1024 - (731 * v) / 1024).coerceIn(0, 255)
                    val b = (yVal + (1815 * u) / 1024).coerceIn(0, 255)

                    pixels[row * width + col] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "YUV conversion failed", e)
            return null
        }
    }

    fun saveBitmapToUri(
        bitmap: Bitmap,
        uri: Uri,
        contentResolver: ContentResolver
    ): Boolean {
        return try {
            contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            } != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save MFS result", e)
            false
        }
    }

    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save MFS result to file", e)
            false
        }
    }
}
