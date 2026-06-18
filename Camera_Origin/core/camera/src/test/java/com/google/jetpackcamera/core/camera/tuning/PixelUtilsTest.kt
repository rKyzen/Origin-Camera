package com.google.jetpackcamera.core.camera.tuning

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PixelUtilsTest {

    @Test
    fun computeHistogram_allZeros_returnsAllInFirstBin() {
        val values = FloatArray(100) { 0f }
        val histogram = PixelUtils.computeHistogram(values)
        assertEquals(100, histogram[0])
        for (i in 1 until histogram.size) {
            assertEquals(0, histogram[i])
        }
    }

    @Test
    fun computeHistogram_allMax_returnsAllInLastBin() {
        val values = FloatArray(100) { 255f }
        val histogram = PixelUtils.computeHistogram(values)
        assertEquals(100, histogram[histogram.size - 1])
        for (i in 0 until histogram.size - 1) {
            assertEquals(0, histogram[i])
        }
    }

    @Test
    fun computeHistogram_uniformSpread_spansMultipleBins() {
        val values = FloatArray(256) { it.toFloat() }
        val histogram = PixelUtils.computeHistogram(values)
        val nonZeroBins = histogram.count { it > 0 }
        assertTrue("Expected multiple bins, got $nonZeroBins", nonZeroBins > 10)
    }

    @Test
    fun mean_emptyArray_returnsZero() {
        assertEquals(0f, PixelUtils.mean(floatArrayOf()), 0.001f)
    }

    @Test
    fun mean_singleValue_returnsThatValue() {
        assertEquals(42f, PixelUtils.mean(floatArrayOf(42f)), 0.001f)
    }

    @Test
    fun mean_uniformValues_returnsThatValue() {
        assertEquals(50f, PixelUtils.mean(FloatArray(100) { 50f }), 0.001f)
    }

    @Test
    fun median_oddCount_returnsMiddle() {
        assertEquals(3f, PixelUtils.median(floatArrayOf(1f, 2f, 3f, 4f, 5f)), 0.001f)
    }

    @Test
    fun median_evenCount_returnsAverageOfMiddle() {
        assertEquals(2.5f, PixelUtils.median(floatArrayOf(1f, 2f, 3f, 4f)), 0.001f)
    }

    @Test
    fun standardDeviation_uniformValues_returnsZero() {
        assertEquals(0f, PixelUtils.standardDeviation(FloatArray(100) { 50f }, 50f), 0.001f)
    }

    @Test
    fun standardDeviation_nonZeroVariance_returnsPositive() {
        val values = floatArrayOf(10f, 20f, 30f, 40f, 50f)
        val mean = PixelUtils.mean(values)
        val stdDev = PixelUtils.standardDeviation(values, mean)
        assertTrue("Expected positive std dev, got $stdDev", stdDev > 0f)
    }

    @Test
    fun computePercentile_histogram_returnsValueInRange() {
        val histogram = IntArray(256)
        histogram[128] = 100
        val p50 = PixelUtils.computePercentile(histogram, 50f)
        assertTrue("Expected percentile between 0-255, got $p50", p50 in 0f..255f)
    }

    @Test
    fun extractLuminance_validBitmap_returnsCorrectSize() {
        val bitmap = createTestBitmap(10, 10, 128, 128, 128)
        val luminance = PixelUtils.extractLuminance(bitmap)
        assertEquals(100, luminance.size)
        assertTrue("Expected luminance ~128, got ${luminance[0]}", luminance[0] in 120f..136f)
        bitmap.recycle()
    }

    @Test
    fun extractRgb_validBitmap_returnsThreeArrays() {
        val bitmap = createTestBitmap(10, 10, 200, 100, 50)
        val (r, g, b) = PixelUtils.extractRgb(bitmap)
        assertEquals(100, r.size)
        assertEquals(100, g.size)
        assertEquals(100, b.size)
        assertTrue("Expected R ~200, got ${r[0]}", r[0] in 190f..210f)
        assertTrue("Expected G ~100, got ${g[0]}", g[0] in 90f..110f)
        assertTrue("Expected B ~50, got ${b[0]}", b[0] in 40f..60f)
        bitmap.recycle()
    }

    private fun createTestBitmap(w: Int, h: Int, r: Int, g: Int, b: Int): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val color = android.graphics.Color.rgb(r, g, b)
        for (y in 0 until h) {
            for (x in 0 until w) {
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }
}
