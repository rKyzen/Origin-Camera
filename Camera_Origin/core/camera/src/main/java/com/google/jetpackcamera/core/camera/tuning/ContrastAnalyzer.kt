package com.google.jetpackcamera.core.camera.tuning

import android.graphics.Bitmap
import kotlin.math.sqrt

object ContrastAnalyzer {

    fun analyze(bitmap: Bitmap): ContrastMetrics {
        val luminance = PixelUtils.extractLuminance(bitmap)
        val mean = PixelUtils.mean(luminance)
        val stdDev = PixelUtils.standardDeviation(luminance, mean)

        val rmsContrast = if (mean > 0f) stdDev / mean else 0f

        val minLum = PixelUtils.min(luminance)
        val maxLum = PixelUtils.max(luminance)
        val michelsonContrast = if (maxLum + minLum > 0f) {
            (maxLum - minLum) / (maxLum + minLum)
        } else 0f

        val localContrast = computeLocalContrast(bitmap)
        return ContrastMetrics(
            rmsContrast = rmsContrast.coerceIn(0f, 1f),
            michelsonContrast = michelsonContrast.coerceIn(0f, 1f),
            localContrast = localContrast.coerceIn(0f, 1f)
        )
    }

    private fun computeLocalContrast(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val blockSize = 8
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var totalContrast = 0f
        var blockCount = 0
        var y = 0
        while (y < height - blockSize) {
            var x = 0
            while (x < width - blockSize) {
                var blockSum = 0f
                var blockSumSq = 0f
                var count = 0
                for (by in 0 until blockSize) {
                    for (bx in 0 until blockSize) {
                        val pixel = pixels[(y + by) * width + (x + bx)]
                        val gray = 0.299f * ((pixel shr 16) and 0xFF) +
                            0.587f * ((pixel shr 8) and 0xFF) +
                            0.114f * (pixel and 0xFF)
                        blockSum += gray
                        blockSumSq += gray * gray
                        count++
                    }
                }
                val blockMean = blockSum / count
                val blockVar = blockSumSq / count - blockMean * blockMean
                totalContrast += sqrt(blockVar.coerceAtLeast(0f))
                blockCount++
                x += blockSize
            }
            y += blockSize
        }
        return if (blockCount > 0) (totalContrast / blockCount) / 255f else 0f
    }
}
