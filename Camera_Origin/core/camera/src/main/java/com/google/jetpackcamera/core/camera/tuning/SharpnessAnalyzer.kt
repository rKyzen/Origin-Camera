package com.google.jetpackcamera.core.camera.tuning

import android.graphics.Bitmap
import kotlin.math.sqrt

object SharpnessAnalyzer {

    private val LAPLACIAN_KERNEL = arrayOf(
        floatArrayOf(0f, 1f, 0f),
        floatArrayOf(1f, -4f, 1f),
        floatArrayOf(0f, 1f, 0f)
    )

    private val SOBEL_X = arrayOf(
        floatArrayOf(-1f, 0f, 1f),
        floatArrayOf(-2f, 0f, 2f),
        floatArrayOf(-1f, 0f, 1f)
    )

    private val SOBEL_Y = arrayOf(
        floatArrayOf(-1f, -2f, -1f),
        floatArrayOf(0f, 0f, 0f),
        floatArrayOf(1f, 2f, 1f)
    )

    fun analyze(bitmap: Bitmap): SharpnessMetrics {
        val laplacian = PixelUtils.applyConvolution(bitmap, LAPLACIAN_KERNEL)
        val lapMean = PixelUtils.mean(laplacian)
        val lapVar = PixelUtils.standardDeviation(laplacian, lapMean)
        val sobelX = PixelUtils.applyConvolution(bitmap, SOBEL_X)
        val sobelY = PixelUtils.applyConvolution(bitmap, SOBEL_Y)
        var totalMag = 0f
        for (i in sobelX.indices) {
            totalMag += sqrt(sobelX[i] * sobelX[i] + sobelY[i] * sobelY[i])
        }
        val sobelMag = if (sobelX.isNotEmpty()) totalMag / sobelX.size else 0f
        return SharpnessMetrics(
            laplacianVariance = lapVar,
            sobelMagnitude = sobelMag / 255f
        )
    }
}
