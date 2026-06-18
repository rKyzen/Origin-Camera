package com.google.jetpackcamera.core.camera.tuning

import android.graphics.Bitmap

object LuminanceAnalyzer {

    fun analyze(bitmap: Bitmap): LuminanceMetrics {
        val luminance = PixelUtils.extractLuminance(bitmap)
        val histogram = PixelUtils.computeHistogram(luminance)
        val mean = PixelUtils.mean(luminance)
        val median = PixelUtils.median(luminance)
        val stdDev = PixelUtils.standardDeviation(luminance, mean)
        return LuminanceMetrics(
            mean = mean,
            median = median,
            standardDeviation = stdDev,
            histogram = histogram,
            percentile1 = PixelUtils.computePercentile(histogram, 1f),
            percentile5 = PixelUtils.computePercentile(histogram, 5f),
            percentile50 = PixelUtils.computePercentile(histogram, 50f),
            percentile95 = PixelUtils.computePercentile(histogram, 95f),
            percentile99 = PixelUtils.computePercentile(histogram, 99f)
        )
    }
}
