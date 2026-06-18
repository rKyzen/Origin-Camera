package com.google.jetpackcamera.core.camera.tuning

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.sqrt

object NoiseEstimator {

    fun estimate(bitmap: Bitmap): NoiseMetrics {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalNoise = 0f
        var blockCount = 0
        val blockSize = 8

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
                if (blockVar > 0f) {
                    totalNoise += sqrt(blockVar)
                    blockCount++
                }

                x += blockSize
            }
            y += blockSize
        }

        val noiseLevel = if (blockCount > 0) totalNoise / blockCount else 0f
        val luminance = PixelUtils.extractLuminance(bitmap)
        val signalMean = PixelUtils.mean(luminance)
        val snr = if (noiseLevel > 0f) signalMean / noiseLevel else Float.MAX_VALUE

        return NoiseMetrics(
            estimatedNoise = (noiseLevel / 255f).coerceIn(0f, 1f),
            signalToNoiseRatio = snr.coerceIn(0f, 100f)
        )
    }
}
