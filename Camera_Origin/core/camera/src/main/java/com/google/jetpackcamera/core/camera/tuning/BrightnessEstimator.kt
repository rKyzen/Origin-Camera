package com.google.jetpackcamera.core.camera.tuning

import android.graphics.Bitmap
import kotlin.math.pow
import kotlin.math.sqrt

object BrightnessEstimator {

    fun estimate(bitmap: Bitmap): Float {
        val luminance = PixelUtils.extractLuminance(bitmap)
        val bt601 = PixelUtils.mean(luminance) / 255f
        val cieLStar = computeCieLStar(luminance)
        return (bt601 * 0.5f + cieLStar * 0.5f).coerceIn(0f, 1f)
    }

    private fun computeCieLStar(luminance: FloatArray): Float {
        val yMean = PixelUtils.mean(luminance) / 255f
        val yNormalized = yMean.coerceIn(0f, 1f)
        val lStar = if (yNormalized > 0.008856f) {
            116f * yNormalized.pow(1f / 3f) - 16f
        } else {
            903.3f * yNormalized
        }
        return (lStar / 100f).coerceIn(0f, 1f)
    }
}
