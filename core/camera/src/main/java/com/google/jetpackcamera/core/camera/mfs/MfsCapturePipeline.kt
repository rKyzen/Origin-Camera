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
    suspend fun captureAndMerge(
        imageCapture: ImageCapture,
        executor: java.util.concurrent.Executor,
        config: MfsConfig
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            val frames = captureBurst(imageCapture, executor, config)
            if (frames.isEmpty()) {
                return@withContext Result.failure(
                    RuntimeException("MFS: no frames captured")
                )
            }
            if (frames.size == 1) {
                val corrected = distortionCorrector?.correct(frames.first().bitmap)
                    ?: frames.first().bitmap
                val contrasted = merger.adjustContrast(corrected, 0.12f)
                val saturated = merger.boostSaturation(contrasted, 1.18f)
                return@withContext Result.success(saturated)
            }

            val alignFrames = if (config.preFilterStrength > 0f) {
                frames.map { it.copy(bitmap = merger.preFilterFrame(it.bitmap)) }
            } else {
                frames
            }
            val reference = alignFrames.first()
            val targets = alignFrames.drop(1)

            val aligned = aligner.alignFrames(reference, targets)
            val allFrames = listOf(reference) + aligned
            val merged = merger.merge(allFrames, config.mergeStrategy)
            val denoised = merger.gentleDenoise(merged, config.denoiseStrength)
            val corrected = distortionCorrector?.correct(denoised) ?: denoised
            val contrasted = merger.adjustContrast(corrected, 0.12f)
            val saturated = merger.boostSaturation(contrasted, 1.18f)
            val sharpened = merger.lightSharpen(saturated, config.sharpenStrength)

            Log.d(
                TAG,
                "MFS done: ${frames.size}f " +
                    "strat=${config.mergeStrategy} " +
                    "preF=${config.preFilterStrength} " +
                    "denoise=${config.denoiseStrength} " +
                    "sharpen=${config.sharpenStrength}"
            )
            Result.success(sharpened)
        } catch (e: Exception) {
            Log.e(TAG, "MFS pipeline failed", e)
            Result.failure(e)
        }
    }

    private suspend fun captureBurst(
        imageCapture: ImageCapture,
        executor: java.util.concurrent.Executor,
        config: MfsConfig
    ): List<FrameData> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val frames = coroutineScope {
            val deferred = (0 until config.frameCount).map { i ->
                async {
                    captureSingleFrame(imageCapture, executor, i)
                }
            }
            deferred.mapNotNull { it.await() }
        }
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Captured ${frames.size}/${config.frameCount} frames in ${elapsed}ms")
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
            val yPlane = imageProxy.planes[0]
            val uPlane = imageProxy.planes[1]
            val vPlane = imageProxy.planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)

            val uvOffset = ySize
            val uvLen = minOf(uSize, vSize)
            for (i in 0 until uvLen) {
                nv21[uvOffset + i * 2] = vBuffer.get(i)
                nv21[uvOffset + i * 2 + 1] = uBuffer.get(i)
            }

            val width = imageProxy.width
            val height = imageProxy.height
            val yuvImage = android.graphics.YuvImage(
                nv21, ImageFormat.NV21, width, height, null
            )
            val tempFile = File.createTempFile("yuv", ".jpg")
            FileOutputStream(tempFile).use { out ->
                yuvImage.compressToJpeg(
                    android.graphics.Rect(0, 0, width, height), 90, out
                )
            }
            val decoded = BitmapFactory.decodeFile(tempFile.absolutePath)
            tempFile.delete()
            return decoded
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
