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
class AnalyzerTest {

    @Test
    fun luminanceAnalyzer_uniformImage_returnsConsistentMetrics() {
        val bitmap = createSolidBitmap(100, 100, 128, 128, 128)
        val metrics = LuminanceAnalyzer.analyze(bitmap)
        assertTrue("Expected mean near 128, got ${metrics.mean}", metrics.mean in 125f..131f)
        assertEquals(0f, metrics.standardDeviation, 0.01f)
        bitmap.recycle()
    }

    @Test
    fun luminanceAnalyzer_brightImage_higherMeanThanDark() {
        val bright = createSolidBitmap(50, 50, 220, 220, 220)
        val dark = createSolidBitmap(50, 50, 50, 50, 50)
        val brightMetrics = LuminanceAnalyzer.analyze(bright)
        val darkMetrics = LuminanceAnalyzer.analyze(dark)
        assertTrue(
            "Bright mean ${brightMetrics.mean} should exceed dark ${darkMetrics.mean}",
            brightMetrics.mean > darkMetrics.mean
        )
        bright.recycle()
        dark.recycle()
    }

    @Test
    fun contrastAnalyzer_uniformImage_zeroRmsContrast() {
        val bitmap = createSolidBitmap(50, 50, 128, 128, 128)
        val metrics = ContrastAnalyzer.analyze(bitmap)
        assertEquals(0f, metrics.rmsContrast, 0.01f)
        bitmap.recycle()
    }

    @Test
    fun contrastAnalyzer_mixedImage_positiveContrast() {
        val bitmap = createGradientBitmap(100, 100)
        val metrics = ContrastAnalyzer.analyze(bitmap)
        assertTrue("Expected positive RMS contrast, got ${metrics.rmsContrast}", metrics.rmsContrast > 0f)
        assertTrue("Expected positive Michelson, got ${metrics.michelsonContrast}", metrics.michelsonContrast > 0f)
        bitmap.recycle()
    }

    @Test
    fun saturationAnalyzer_greyImage_lowSaturation() {
        val bitmap = createSolidBitmap(50, 50, 128, 128, 128)
        val metrics = SaturationAnalyzer.analyze(bitmap)
        assertTrue("Expected low saturation for grey, got ${metrics.meanSaturation}", metrics.meanSaturation < 0.05f)
        bitmap.recycle()
    }

    @Test
    fun saturationAnalyzer_coloredImage_higherSaturation() {
        val bitmap = createSolidBitmap(50, 50, 255, 0, 0)
        val metrics = SaturationAnalyzer.analyze(bitmap)
        assertTrue("Expected higher saturation for red, got ${metrics.meanSaturation}", metrics.meanSaturation > 0.5f)
        bitmap.recycle()
    }

    @Test
    fun sharpnessAnalyzer_uniformImage_lowSharpness() {
        val bitmap = createSolidBitmap(50, 50, 128, 128, 128)
        val metrics = SharpnessAnalyzer.analyze(bitmap)
        assertEquals(0f, metrics.laplacianVariance, 0.01f)
        bitmap.recycle()
    }

    @Test
    fun sharpnessAnalyzer_edgedImage_higherSharpness() {
        val bitmap = createEdgeBitmap(100, 100)
        val metrics = SharpnessAnalyzer.analyze(bitmap)
        assertTrue("Expected positive sharpness for edges, got ${metrics.laplacianVariance}", metrics.laplacianVariance > 0f)
        bitmap.recycle()
    }

    @Test
    fun noiseEstimator_uniformImage_lowNoise() {
        val bitmap = createSolidBitmap(50, 50, 128, 128, 128)
        val metrics = NoiseEstimator.estimate(bitmap)
        assertTrue("Expected low noise for uniform image, got ${metrics.estimatedNoise}", metrics.estimatedNoise < 0.01f)
        bitmap.recycle()
    }

    @Test
    fun brightnessEstimator_brightImage_higherThanDark() {
        val bright = createSolidBitmap(50, 50, 220, 220, 220)
        val dark = createSolidBitmap(50, 50, 40, 40, 40)
        val brightB = BrightnessEstimator.estimate(bright)
        val darkB = BrightnessEstimator.estimate(dark)
        assertTrue("Bright $brightB should exceed dark $darkB", brightB > darkB)
        bright.recycle()
        dark.recycle()
    }

    @Test
    fun balancedProfileCalculator_returnsMidpoint() {
        val stockMetrics = ImageMetrics(
            luminance = LuminanceMetrics(180f, 180f, 0f, IntArray(256), 180f, 180f, 180f, 180f, 180f),
            contrast = ContrastMetrics(0.4f, 0.8f, 0.3f),
            saturation = SaturationMetrics(0.6f, 0.9f, 0.2f),
            sharpness = SharpnessMetrics(10f, 0.1f),
            noise = NoiseMetrics(0.02f, 50f),
            perceivedBrightness = 0.7f
        )
        val cameraMetrics = ImageMetrics(
            luminance = LuminanceMetrics(120f, 120f, 0f, IntArray(256), 120f, 120f, 120f, 120f, 120f),
            contrast = ContrastMetrics(0.25f, 0.5f, 0.2f),
            saturation = SaturationMetrics(0.4f, 0.7f, 0.1f),
            sharpness = SharpnessMetrics(8f, 0.08f),
            noise = NoiseMetrics(0.015f, 60f),
            perceivedBrightness = 0.45f
        )
        val profile = BalancedProfileCalculator.calculate(stockMetrics, cameraMetrics)
        assertTrue(
            "Target brightness should be between stock and camera",
            profile.targetBrightness in 0.45f..0.7f
        )
        assertTrue(
            "Target saturation should be reasonable, got ${profile.targetSaturation}",
            profile.targetSaturation in 0.3f..0.7f
        )
    }

    @Test
    fun sceneComparison_generatesReport() {
        val stock = createSolidBitmap(50, 50, 200, 200, 200)
        val camera = createSolidBitmap(50, 50, 100, 100, 100)
        val stockMetrics = TuningAnalyzer().analyzeImage(stock)
        val cameraMetrics = TuningAnalyzer().analyzeImage(camera)
        val report = SceneComparison.compare("test_scene", stockMetrics, cameraMetrics)
        assertEquals("test_scene", report.sceneName)
        assertTrue("Expected recommendations", report.recommendations.isNotEmpty())
        stock.recycle()
        camera.recycle()
    }

    @Test
    fun reportGenerator_producesNonEmptyOutput() {
        val stock = createSolidBitmap(50, 50, 200, 200, 200)
        val camera = createSolidBitmap(50, 50, 100, 100, 100)
        val stockMetrics = TuningAnalyzer().analyzeImage(stock)
        val cameraMetrics = TuningAnalyzer().analyzeImage(camera)
        val report = SceneComparison.compare("test", stockMetrics, cameraMetrics)
        val output = ReportGenerator.generate(report)
        assertTrue("Report should not be empty", output.isNotEmpty())
        assertTrue("Report should contain scene name", output.contains("test"))
        stock.recycle()
        camera.recycle()
    }

    private fun createSolidBitmap(w: Int, h: Int, r: Int, g: Int, b: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val color = Color.rgb(r, g, b)
        for (y in 0 until h) for (x in 0 until w) bitmap.setPixel(x, y, color)
        return bitmap
    }

    private fun createGradientBitmap(w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (y in 0 until h) {
            val gray = (y * 255 / h)
            val color = Color.rgb(gray, gray, gray)
            for (x in 0 until w) bitmap.setPixel(x, y, color)
        }
        return bitmap
    }

    private fun createEdgeBitmap(w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val midX = w / 2
        for (y in 0 until h) {
            for (x in 0 until w) {
                val color = if (x < midX) Color.WHITE else Color.BLACK
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }
}
