package com.google.jetpackcamera.core.camera.mfs

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrameMergerTest {

    private lateinit var merger: FrameMerger

    @Before
    fun setUp() {
        merger = FrameMerger()
    }

    @Test
    fun averageMerge_twoSolidFrames_returnsCorrectAverage() {
        val w = 4
        val h = 4
        val black = createSolidBitmap(w, h, 0, 0, 0)
        val white = createSolidBitmap(w, h, 255, 255, 255)
        val frames = listOf(
            FrameData(black, 0, 0L),
            FrameData(white, 1, 1L)
        )
        val result = merger.merge(frames, MergeStrategy.AVERAGE)
        val pixels = IntArray(w * h)
        result.getPixels(pixels, 0, w, 0, 0, w, h)
        val sample = pixels[0]
        assertThat(sample shr 16 and 0xFF).isEqualTo(127)
        assertThat(sample shr 8 and 0xFF).isEqualTo(127)
        assertThat(sample and 0xFF).isEqualTo(127)
    }

    @Test
    fun weightedMerge_sharperFrameGetsHigherWeight() {
        val w = 16
        val h = 16
        val sharp = createCheckerBitmap(w, h)
        val blur = createSolidBitmap(w, h, 128, 128, 128)
        val frames = listOf(
            FrameData(blur, 0, 0L),
            FrameData(sharp, 1, 1L)
        )
        val result = merger.merge(frames, MergeStrategy.WEIGHTED)
        val pixels = IntArray(w * h)
        result.getPixels(pixels, 0, w, 0, 0, w, h)
        val sample = pixels[0]
        assertThat((sample shr 16 and 0xFF) >= 64).isTrue()
        assertThat((sample and 0xFF) >= 64).isTrue()
    }

    @Test
    fun motionAwareMerge_identicalFrames_producesSameImage() {
        val w = 8
        val h = 8
        val bmp = createGradientBitmap(w, h)
        val frames = listOf(
            FrameData(bmp, 0, 0L),
            FrameData(bmp.copy(bmp.config!!, true), 1, 1L).also {
                it.bitmap.setPixels(getPixels(bmp, w, h), 0, w, 0, 0, w, h)
            }
        )
        val result = merger.merge(frames, MergeStrategy.MOTION_AWARE)
        val refPixels = getPixels(bmp, w, h)
        val outPixels = getPixels(result, w, h)
        assertThat(outPixels).isEqualTo(refPixels)
    }

    @Test
    fun gentleDenoise_zeroStrength_returnsSameBitmap() {
        val bmp = createSolidBitmap(8, 8, 100, 150, 200)
        val result = merger.gentleDenoise(bmp, 0f)
        assertThat(result).isSameInstanceAs(bmp)
    }

    @Test
    fun gentleDenoise_positiveStrength_smoothensImage() {
        val w = 16
        val h = 16
        val bmp = createSolidBitmap(w, h, 200, 100, 50)
        val noisy = addNoise(bmp, w, h, 40)
        val denoised = merger.gentleDenoise(noisy, 0.5f)
        val noisyPixels = getPixels(noisy, w, h)
        val denoisedPixels = getPixels(denoised, w, h)
        var noisyVar = 0f
        var denoisedVar = 0f
        val mean = 200f
        for (i in noisyPixels.indices) {
            val nr = noisyPixels[i] shr 16 and 0xFF
            val dr = denoisedPixels[i] shr 16 and 0xFF
            noisyVar += (nr - mean) * (nr - mean)
            denoisedVar += (dr - mean) * (dr - mean)
        }
        noisyVar /= noisyPixels.size
        denoisedVar /= noisyPixels.size
        assertThat(denoisedVar < noisyVar).isTrue()
    }

    @Test
    fun lightSharpen_zeroStrength_returnsSameBitmap() {
        val bmp = createSolidBitmap(8, 8, 100, 150, 200)
        val result = merger.lightSharpen(bmp, 0f)
        assertThat(result).isSameInstanceAs(bmp)
    }

    @Test
    fun lightSharpen_positiveStrength_increasesEdges() {
        val w = 16
        val h = 16
        val edge = createEdgeBitmap(w, h)
        val sharpened = merger.lightSharpen(edge, 0.8f)
        val srcPixels = getPixels(edge, w, h)
        val dstPixels = getPixels(sharpened, w, h)
        var edgeIncrease = 0
        for (i in srcPixels.indices) {
            val sr = srcPixels[i] shr 16 and 0xFF
            val dr = dstPixels[i] shr 16 and 0xFF
            if (dr > sr) edgeIncrease++
        }
        assertThat(edgeIncrease > 0).isTrue()
    }

    @Test
    fun adjustContrast_positiveAmount_modifiesPixels() {
        val bmp = createSolidBitmap(4, 4, 80, 80, 80)
        val result = merger.adjustContrast(bmp, 0.12f)
        val pixels = getPixels(result, 4, 4)
        val r = pixels[0] shr 16 and 0xFF
        assertThat(r).isNotEqualTo(80)
    }

    @Test
    fun boostSaturation_factorOne_returnsSameBitmap() {
        val bmp = createSolidBitmap(4, 4, 100, 150, 200)
        val result = merger.boostSaturation(bmp, 1f)
        assertThat(result).isSameInstanceAs(bmp)
    }

    @Test
    fun boostSaturation_validFactor_increasesSaturation() {
        val w = 4
        val h = 4
        val bmp = createSolidBitmap(w, h, 200, 100, 50)
        val result = merger.boostSaturation(bmp, 1.5f)
        val src = getPixels(bmp, w, h)
        val dst = getPixels(result, w, h)
        val srcGray = (200 * 77 + 100 * 150 + 50 * 29) shr 8
        val dstGray = (dst[0] shr 16 and 0xFF) * 77 +
            (dst[0] shr 8 and 0xFF) * 150 +
            (dst[0] and 0xFF) * 29 shr 8
        val srcSat = abs(200 - srcGray) + abs(100 - srcGray) + abs(50 - srcGray)
        val dstSat = abs((dst[0] shr 16 and 0xFF) - dstGray) +
            abs((dst[0] shr 8 and 0xFF) - dstGray) +
            abs((dst[0] and 0xFF) - dstGray)
        assertThat(dstSat > srcSat).isTrue()
    }

    @Test
    fun singleFrameMerge_returnsFrameDirectly() {
        val bmp = createSolidBitmap(4, 4, 100, 120, 140)
        val frames = listOf(FrameData(bmp, 0, 0L))
        val result = merger.merge(frames, MergeStrategy.AVERAGE)
        val refPixels = getPixels(bmp, 4, 4)
        val outPixels = getPixels(result, 4, 4)
        assertThat(outPixels).isEqualTo(refPixels)
    }

    @Test
    fun enhanceDetail_zeroStrengths_returnsSameBitmap() {
        val bmp = createSolidBitmap(8, 8, 100, 150, 200)
        val result = merger.enhanceDetail(bmp, 0f, 0f)
        assertThat(result).isSameInstanceAs(bmp)
    }

    @Test
    fun enhanceDetail_denoiseOnly_reducesVariance() {
        val w = 16
        val h = 16
        val bmp = createSolidBitmap(w, h, 200, 100, 50)
        val noisy = addNoise(bmp, w, h, 15)
        val enhanced = merger.enhanceDetail(noisy, 0.3f, 0f)
        val noisyPixels = getPixels(noisy, w, h)
        val enhancedPixels = getPixels(enhanced, w, h)
        var noisyVar = 0f
        var enhancedVar = 0f
        val mean = 200f
        for (i in noisyPixels.indices) {
            val nr = noisyPixels[i] shr 16 and 0xFF
            val dr = enhancedPixels[i] shr 16 and 0xFF
            noisyVar += (nr - mean) * (nr - mean)
            enhancedVar += (dr - mean) * (dr - mean)
        }
        noisyVar /= noisyPixels.size
        enhancedVar /= noisyPixels.size
        assertThat(enhancedVar < noisyVar).isTrue()
    }

    @Test
    fun enhanceDetail_sharpenOnly_increasesEdgeContrast() {
        val w = 16
        val h = 16
        val edge = createEdgeBitmap(w, h)
        val enhanced = merger.enhanceDetail(edge, 0f, 0.8f)
        val srcPixels = getPixels(edge, w, h)
        val dstPixels = getPixels(enhanced, w, h)
        var edgeIncrease = 0
        for (i in srcPixels.indices) {
            val sr = srcPixels[i] shr 16 and 0xFF
            val dr = dstPixels[i] shr 16 and 0xFF
            if (dr > sr) edgeIncrease++
        }
        assertThat(edgeIncrease > 0).isTrue()
    }

    @Test
    fun enhanceDetail_combined_noCrash() {
        val w = 32
        val h = 32
        val bmp = createCheckerBitmap(w, h)
        val result = merger.enhanceDetail(bmp, 0.2f, 0.6f)
        assertThat(result.width).isEqualTo(w)
        assertThat(result.height).isEqualTo(h)
    }

    @Test
    fun enhanceDetail_preservesSolidRegions() {
        val w = 16
        val h = 16
        val bmp = createSolidBitmap(w, h, 128, 128, 128)
        val enhanced = merger.enhanceDetail(bmp, 0.3f, 0.5f)
        val pixels = getPixels(enhanced, w, h)
        for (i in pixels.indices) {
            val r = pixels[i] shr 16 and 0xFF
            val g = pixels[i] shr 8 and 0xFF
            val b = pixels[i] and 0xFF
            assertThat(abs(r - 128) <= 2).isTrue()
            assertThat(abs(g - 128) <= 2).isTrue()
            assertThat(abs(b - 128) <= 2).isTrue()
        }
    }

    private fun createSolidBitmap(w: Int, h: Int, r: Int, g: Int, b: Int): Bitmap {
        val pixels = IntArray(w * h) { (0xFF shl 24) or (r shl 16) or (g shl 8) or b }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun createCheckerBitmap(w: Int, h: Int): Bitmap {
        val pixels = IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            val c = if ((x + y) % 2 == 0) 255 else 0
            (0xFF shl 24) or (c shl 16) or (c shl 8) or c
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun createGradientBitmap(w: Int, h: Int): Bitmap {
        val pixels = IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            val r = (x * 255 / w).coerceIn(0, 255)
            val g = (y * 255 / h).coerceIn(0, 255)
            val b = ((x + y) * 128 / (w + h)).coerceIn(0, 255)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun createEdgeBitmap(w: Int, h: Int): Bitmap {
        val pixels = IntArray(w * h) { i ->
            val x = i % w
            val c = if (x < w / 2) 50 else 200
            (0xFF shl 24) or (c shl 16) or (c shl 8) or c
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun addNoise(bmp: Bitmap, w: Int, h: Int, amount: Int): Bitmap {
        val pixels = getPixels(bmp, w, h)
        val noisy = IntArray(w * h) { i ->
            val r = ((pixels[i] shr 16 and 0xFF) + (Math.random() * amount * 2 - amount).toInt())
                .coerceIn(0, 255)
            val g = ((pixels[i] shr 8 and 0xFF) + (Math.random() * amount * 2 - amount).toInt())
                .coerceIn(0, 255)
            val b = ((pixels[i] and 0xFF) + (Math.random() * amount * 2 - amount).toInt())
                .coerceIn(0, 255)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(noisy, 0, w, 0, 0, w, h)
        return result
    }

    private fun getPixels(bmp: Bitmap, w: Int, h: Int): IntArray {
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        return pixels
    }
}
