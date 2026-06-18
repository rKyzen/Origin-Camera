package com.google.jetpackcamera.core.camera.tuning

import kotlin.math.abs

object BalancedProfileCalculator {

    private const val MIN_BRIGHTNESS_FLOOR = 0.12f
    private const val NATURAL_BIAS = 0.4f
    private const val VIBRANCE_MIN = 0.05f

    fun calculate(stockMetrics: ImageMetrics, cameraMetrics: ImageMetrics): BalancedProfile {
        val targetBrightness = computeBalancedBrightness(
            stockMetrics.perceivedBrightness,
            cameraMetrics.perceivedBrightness
        )
        val targetLuminance = computeBalancedValue(
            stockMetrics.luminance.mean,
            cameraMetrics.luminance.mean,
            255f
        )
        val targetContrast = computeBalancedContrast(
            stockMetrics.contrast,
            cameraMetrics.contrast
        )
        val targetSaturation = computeBalancedSaturation(
            stockMetrics.saturation,
            cameraMetrics.saturation
        )
        val targetSharpness = computeBalancedValue(
            stockMetrics.sharpness.laplacianVariance,
            cameraMetrics.sharpness.laplacianVariance,
            maxOf(stockMetrics.sharpness.laplacianVariance, cameraMetrics.sharpness.laplacianVariance) * 2f
        )
        val targetNoise = computeBalancedNoise(
            stockMetrics.noise,
            cameraMetrics.noise
        )
        return BalancedProfile(
            targetLuminance = targetLuminance,
            targetContrast = targetContrast,
            targetSaturation = targetSaturation,
            targetSharpness = targetSharpness,
            targetNoise = targetNoise,
            targetBrightness = targetBrightness
        )
    }

    private fun computeBalancedBrightness(stockBrightness: Float, cameraBrightness: Float): Float {
        val rawBalanced = (stockBrightness + cameraBrightness) / 2f
        val diff = stockBrightness - cameraBrightness
        val boostedCamera = cameraBrightness + diff * NATURAL_BIAS
        var balanced = (rawBalanced + boostedCamera) / 2f
        if (balanced < MIN_BRIGHTNESS_FLOOR) {
            balanced = MIN_BRIGHTNESS_FLOOR
        }
        return balanced.coerceIn(0f, 1f)
    }

    private fun computeBalancedValue(stock: Float, camera: Float, maxValue: Float): Float {
        val diff = stock - camera
        val naturalShift = diff * NATURAL_BIAS
        return (camera + naturalShift).coerceIn(0f, maxValue)
    }

    private fun computeBalancedContrast(stock: ContrastMetrics, camera: ContrastMetrics): Float {
        val stockRms = stock.rmsContrast
        val cameraRms = camera.rmsContrast
        val rawBalanced = (stockRms + cameraRms) / 2f
        val localTarget = (stock.localContrast + camera.localContrast) / 2f
        val combined = rawBalanced * 0.6f + localTarget * 0.4f
        return combined.coerceIn(0f, 1f)
    }

    private fun computeBalancedSaturation(stock: SaturationMetrics, camera: SaturationMetrics): Float {
        val rawBalanced = (stock.meanSaturation + camera.meanSaturation) / 2f
        val vibranceTarget = (stock.vibrance + camera.vibrance) / 2f
        var balanced = rawBalanced * 0.65f + vibranceTarget * 0.35f
        if (balanced < VIBRANCE_MIN) {
            balanced = VIBRANCE_MIN
        }
        return balanced.coerceIn(0f, 1f)
    }

    private fun computeBalancedNoise(stock: NoiseMetrics, camera: NoiseMetrics): Float {
        return (stock.estimatedNoise + camera.estimatedNoise) / 2f
    }
}
