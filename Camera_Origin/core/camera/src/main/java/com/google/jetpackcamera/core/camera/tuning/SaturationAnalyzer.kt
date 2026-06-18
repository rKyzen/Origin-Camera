package com.google.jetpackcamera.core.camera.tuning

import android.graphics.Bitmap
import kotlin.math.max

object SaturationAnalyzer {

    fun analyze(bitmap: Bitmap): SaturationMetrics {
        val (r, g, b) = PixelUtils.extractRgb(bitmap)
        val n = r.size
        var totalSat = 0f
        var maxSat = 0f
        var sumOfMaxMinusMean = 0f
        var meanSat = 0f

        val saturations = FloatArray(n)
        for (i in 0 until n) {
            val maxC = max(r[i], max(g[i], b[i]))
            val minC = minOf(r[i], g[i], b[i])
            val sat = if (maxC > 0f) (maxC - minC) / maxC else 0f
            saturations[i] = sat
            totalSat += sat
            if (sat > maxSat) maxSat = sat
        }
        meanSat = totalSat / n
        for (i in 0 until n) {
            val boost = saturations[i] - meanSat
            if (boost > 0f) sumOfMaxMinusMean += boost
        }
        val vibrance = if (n > 0) sumOfMaxMinusMean / n else 0f
        return SaturationMetrics(
            meanSaturation = meanSat.coerceIn(0f, 1f),
            maxSaturation = maxSat.coerceIn(0f, 1f),
            vibrance = vibrance.coerceIn(0f, 1f)
        )
    }
}
