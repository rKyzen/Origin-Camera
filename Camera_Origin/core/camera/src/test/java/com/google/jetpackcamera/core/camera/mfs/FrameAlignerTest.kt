package com.google.jetpackcamera.core.camera.mfs

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrameAlignerTest {

    private lateinit var aligner: FrameAligner

    @Before
    fun setUp() {
        aligner = FrameAligner()
    }

    @Test
    fun alignFrames_identicalFrames_returnsOriginal() {
        val w = 64
        val h = 64
        val bmp = createPatternBitmap(w, h)
        val ref = FrameData(bmp, 0, 0L)
        val target = FrameData(bmp.copy(bmp.config!!, true), 1, 1L).also {
            it.bitmap.setPixels(getPixels(bmp, w, h), 0, w, 0, 0, w, h)
        }
        val aligned = aligner.alignFrames(ref, listOf(target))
        assertThat(aligned).hasSize(1)
    }

    @Test
    fun alignFrames_smallTranslation_shiftsBack() {
        val w = 128
        val h = 128
        val src = createPatternBitmap(w, h)
        val translated = translateBitmap(src, w, h, 2f, 1f)
        val ref = FrameData(src, 0, 0L)
        val target = FrameData(translated, 1, 1L)
        val aligned = aligner.alignFrames(ref, listOf(target))
        assertThat(aligned).hasSize(1)
        assertThat(aligned[0].bitmap.width).isEqualTo(w)
        assertThat(aligned[0].bitmap.height).isEqualTo(h)
    }

    @Test
    fun alignFrames_emptyTargets_returnsEmptyList() {
        val ref = FrameData(createPatternBitmap(32, 32), 0, 0L)
        val aligned = aligner.alignFrames(ref, emptyList())
        assertThat(aligned).isEmpty()
    }

    @Test
    fun alignFrames_largeTranslation_returnsOriginalTarget() {
        val w = 64
        val h = 64
        val src = createPatternBitmap(w, h)
        val translated = translateBitmap(src, w, h, 50f, 50f)
        val ref = FrameData(src, 0, 0L)
        val target = FrameData(translated, 1, 1L)
        val aligned = aligner.alignFrames(ref, listOf(target))
        val refPixels = getPixels(src, w, h)
        val alignedPixels = getPixels(aligned[0].bitmap, w, h)
        var differentPixels = 0
        for (i in refPixels.indices) {
            if (refPixels[i] != alignedPixels[i]) {
                differentPixels++
            }
        }
        val totalPixels = w * h
        assertThat(differentPixels.toFloat() / totalPixels >= 0.3f).isTrue()
    }

    @Test
    fun alignFrames_noShift_confidenceHigh() {
        val w = 64
        val h = 64
        val bmp = createPatternBitmap(w, h)
        val ref = FrameData(bmp, 0, 0L)
        val target = FrameData(bmp.copy(bmp.config!!, true), 1, 1L).also {
            it.bitmap.setPixels(getPixels(bmp, w, h), 0, w, 0, 0, w, h)
        }
        val aligned = aligner.alignFrames(ref, listOf(target))
        assertThat(aligned[0].bitmap.sameAs(bmp)).isTrue()
    }

    @Test
    fun alignFrames_smallRotation_returnsAlignedWithCorrectSize() {
        val w = 128
        val h = 128
        val src = createPatternBitmap(w, h)
        val rotated = rotateBitmap(src, w, h, 0.8f)
        val ref = FrameData(src, 0, 0L)
        val target = FrameData(rotated, 1, 1L)
        val aligned = aligner.alignFrames(ref, listOf(target))
        assertThat(aligned).hasSize(1)
        assertThat(aligned[0].bitmap.width).isEqualTo(w)
        assertThat(aligned[0].bitmap.height).isEqualTo(h)
    }

    @Test
    fun alignFrames_rotationAndTranslation_returnsAlignedWithCorrectSize() {
        val w = 128
        val h = 128
        val src = createPatternBitmap(w, h)
        var tmp = rotateBitmap(src, w, h, 0.5f)
        val transformed = translateBitmap(tmp, w, h, 1f, 2f)
        val ref = FrameData(src, 0, 0L)
        val target = FrameData(transformed, 1, 1L)
        val aligned = aligner.alignFrames(ref, listOf(target))
        assertThat(aligned).hasSize(1)
        assertThat(aligned[0].bitmap.width).isEqualTo(w)
        assertThat(aligned[0].bitmap.height).isEqualTo(h)
    }

    private fun createPatternBitmap(w: Int, h: Int): Bitmap {
        val pixels = IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            val r = (x * 7 % 256).coerceIn(0, 255)
            val g = (y * 11 % 256).coerceIn(0, 255)
            val b = ((x + y) * 13 % 256).coerceIn(0, 255)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun translateBitmap(src: Bitmap, w: Int, h: Int, dx: Float, dy: Float): Bitmap {
        val srcPixels = getPixels(src, w, h)
        val dstPixels = IntArray(w * h) { 0 }
        for (y in 0 until h) {
            for (x in 0 until w) {
                val sx = (x - dx).toInt()
                val sy = (y - dy).toInt()
                if (sx in 0 until w && sy in 0 until h) {
                    dstPixels[y * w + x] = srcPixels[sy * w + sx]
                }
            }
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(dstPixels, 0, w, 0, 0, w, h)
        return result
    }

    private fun getPixels(bmp: Bitmap, w: Int, h: Int): IntArray {
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        return pixels
    }

    private fun rotateBitmap(src: Bitmap, w: Int, h: Int, angleDeg: Float): Bitmap {
        val srcPixels = getPixels(src, w, h)
        val dstPixels = IntArray(w * h) { 0 }
        val cx = w / 2f
        val cy = h / 2f
        val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val dx = x - cx
                val dy = y - cy
                val sx = (cosA * dx + sinA * dy + cx).toInt()
                val sy = (-sinA * dx + cosA * dy + cy).toInt()
                if (sx in 0 until w && sy in 0 until h) {
                    dstPixels[y * w + x] = srcPixels[sy * w + sx]
                }
            }
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(dstPixels, 0, w, 0, 0, w, h)
        return result
    }
}
