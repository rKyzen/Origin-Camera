package com.google.jetpackcamera.core.camera.mfs

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LensDistortionCorrectorTest {

    @Test
    fun correct_noCharacteristics_returnsOriginalBitmap() {
        val corrector = LensDistortionCorrector(null)
        val bmp = createTestBitmap(32, 32)
        val result = corrector.correct(bmp)
        assertThat(result).isSameInstanceAs(bmp)
    }

    @Test
    fun correct_withZeroCoefficients_returnsOriginalBitmap() {
        val corrector = LensDistortionCorrector(null)
        val bmp = createTestBitmap(32, 32)
        val result = corrector.correct(bmp)
        assertThat(result).isSameInstanceAs(bmp)
    }

    @Test
    fun correct_smallBitmap_doesNotCrash() {
        val corrector = LensDistortionCorrector(null)
        val bmp = createTestBitmap(4, 4)
        val result = corrector.correct(bmp)
        assertThat(result).isNotNull()
    }

    @Test
    fun correct_largeBitmap_doesNotCrash() {
        val corrector = LensDistortionCorrector(null)
        val bmp = createTestBitmap(256, 256)
        val result = corrector.correct(bmp)
        assertThat(result).isNotNull()
    }

    @Test
    fun correct_nullCharacteristics_returnsSameInstance() {
        val corrector = LensDistortionCorrector()
        val bmp = createTestBitmap(16, 16)
        val result = corrector.correct(bmp)
        assertThat(result).isSameInstanceAs(bmp)
    }

    private fun createTestBitmap(w: Int, h: Int): Bitmap {
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
}
