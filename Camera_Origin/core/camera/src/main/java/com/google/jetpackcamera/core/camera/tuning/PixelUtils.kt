package com.google.jetpackcamera.core.camera.tuning

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object PixelUtils {

    fun extractLuminance(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return FloatArray(pixels.size) { i ->
            val r = (pixels[i] shr 16) and 0xFF
            val g = (pixels[i] shr 8) and 0xFF
            val b = pixels[i] and 0xFF
            0.299f * r + 0.587f * g + 0.114f * b
        }
    }

    fun extractRgb(bitmap: Bitmap): Triple<FloatArray, FloatArray, FloatArray> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val r = FloatArray(pixels.size)
        val g = FloatArray(pixels.size)
        val b = FloatArray(pixels.size)
        for (i in pixels.indices) {
            r[i] = ((pixels[i] shr 16) and 0xFF).toFloat()
            g[i] = ((pixels[i] shr 8) and 0xFF).toFloat()
            b[i] = (pixels[i] and 0xFF).toFloat()
        }
        return Triple(r, g, b)
    }

    fun computeHistogram(values: FloatArray, bins: Int = 256): IntArray {
        val histogram = IntArray(bins)
        val scale = (bins - 1).toFloat() / 255f
        for (v in values) {
            val bin = (v * scale).toInt().coerceIn(0, bins - 1)
            histogram[bin]++
        }
        return histogram
    }

    fun computePercentile(histogram: IntArray, percentile: Float): Float {
        val total = histogram.sum()
        val target = (percentile / 100f * total).toInt()
        var cumulative = 0
        for (i in histogram.indices) {
            cumulative += histogram[i]
            if (cumulative >= target) {
                return i.toFloat() * 255f / (histogram.size - 1)
            }
        }
        return 255f
    }

    fun mean(values: FloatArray): Float {
        if (values.isEmpty()) return 0f
        var sum = 0.0
        for (v in values) sum += v
        return (sum / values.size).toFloat()
    }

    fun median(values: FloatArray): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.copyOf()
        sorted.sort()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2f
        } else {
            sorted[mid]
        }
    }

    fun standardDeviation(values: FloatArray, mean: Float): Float {
        if (values.isEmpty()) return 0f
        var sumSq = 0.0
        for (v in values) {
            val diff = v - mean
            sumSq += diff * diff
        }
        return sqrt((sumSq / values.size).toFloat())
    }

    fun applyConvolution(bitmap: Bitmap, kernel: Array<FloatArray>): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val kSize = kernel.size
        val kHalf = kSize / 2
        val result = FloatArray(width * height)
        for (y in kHalf until height - kHalf) {
            for (x in kHalf until width - kHalf) {
                var sum = 0f
                for (ky in 0 until kSize) {
                    for (kx in 0 until kSize) {
                        val px = x + kx - kHalf
                        val py = y + ky - kHalf
                        val pixel = pixels[py * width + px]
                        val gray = 0.299f * ((pixel shr 16) and 0xFF) +
                            0.587f * ((pixel shr 8) and 0xFF) +
                            0.114f * (pixel and 0xFF)
                        sum += kernel[ky][kx] * gray
                    }
                }
                result[y * width + x] = sum
            }
        }
        return result
    }

    fun min(values: FloatArray): Float {
        if (values.isEmpty()) return 0f
        var m = Float.MAX_VALUE
        for (v in values) if (v < m) m = v
        return m
    }

    fun max(values: FloatArray): Float {
        if (values.isEmpty()) return 0f
        var m = Float.MIN_VALUE
        for (v in values) if (v > m) m = v
        return m
    }
}
