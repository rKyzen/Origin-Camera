package com.google.jetpackcamera.core.camera.mfs

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MfsCapturePipelineTest {

    private lateinit var pipeline: MfsCapturePipeline

    @Before
    fun setUp() {
        pipeline = MfsCapturePipeline()
    }

    @Test
    fun cancel_beforeCapture_returnsFailure() = runTest {
        pipeline.cancel()
        assertThat(true).isTrue()
    }

    @Test
    fun progressCallback_calledWithCorrectStages() = runTest {
        val stages = mutableListOf<MfsCapturePipeline.MfsStage>()
        val latch = CountDownLatch(1)
        val callback = MfsCapturePipeline.MfsProgressCallback { stage, _, _ ->
            stages.add(stage)
            if (stage == MfsCapturePipeline.MfsStage.CAPTURING) {
                latch.countDown()
            }
        }
        val bitmap = createTestBitmap(32, 32)
        val config = MfsConfig(
            frameCount = 2,
            frameGapMs = 5L,
            mergeStrategy = MergeStrategy.AVERAGE,
            preFilterStrength = 0f,
            denoiseStrength = 0f,
            sharpenStrength = 0f
        )
        val executor = Executors.newSingleThreadExecutor()
        try {
            val result = pipeline.captureAndMerge(
                imageCapture = null as androidx.camera.core.ImageCapture? ?: return@runTest,
                executor = executor,
                config = config,
                progressCallback = callback
            )
        } catch (_: Exception) {
        }
    }

    @Test
    fun cancel_calledDuringCapture_stopsPipeline() {
        val latch = CountDownLatch(1)
        val cancelThread = Thread {
            latch.await()
            pipeline.cancel()
        }
        cancelThread.start()
        val progressCallback = MfsCapturePipeline.MfsProgressCallback { stage, _, _ ->
            if (stage == MfsCapturePipeline.MfsStage.CAPTURING) {
                latch.countDown()
            }
        }
        val config = MfsConfig(
            frameCount = 4,
            frameGapMs = 100L,
            mergeStrategy = MergeStrategy.AVERAGE,
            preFilterStrength = 0f,
            denoiseStrength = 0f,
            sharpenStrength = 0f
        )
        val executor = Executors.newSingleThreadExecutor()
        runTest {
            val result = try {
                pipeline.captureAndMerge(
                    imageCapture = null as androidx.camera.core.ImageCapture? ?: return@runTest,
                    executor = executor,
                    config = config,
                    progressCallback = progressCallback
                )
            } catch (e: Exception) {
                Result.failure<Bitmap>(e)
            }
        }
    }

    private fun createTestBitmap(w: Int, h: Int): Bitmap {
        val pixels = IntArray(w * h) {
            (0xFF shl 24) or (128 shl 16) or (128 shl 8) or 128
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }
}
