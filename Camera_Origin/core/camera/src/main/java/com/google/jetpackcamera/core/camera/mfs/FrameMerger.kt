package com.google.jetpackcamera.core.camera.mfs

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.util.Log
import kotlin.math.abs
import kotlin.math.exp

private const val TAG = "FrameMerger"

class FrameMerger {

    fun merge(frames: List<FrameData>, strategy: MergeStrategy): Bitmap {
        return when (strategy) {
            MergeStrategy.AVERAGE -> averageMerge(frames)
            MergeStrategy.WEIGHTED -> weightedMerge(frames)
            MergeStrategy.MOTION_AWARE -> motionAwareMerge(frames)
        }
    }

    private fun averageMerge(frames: List<FrameData>): Bitmap {
        val w = frames.first().width
        val h = frames.first().height
        val result = IntArray(w * h)
        val n = frames.size

        for (frame in frames) {
            val pixels = IntArray(w * h)
            frame.bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            for (i in pixels.indices) {
                val pr = pixels[i] shr 16 and 0xFF
                val pg = pixels[i] shr 8 and 0xFF
                val pb = pixels[i] and 0xFF
                val rr = result[i] shr 16 and 0xFF
                val rg = result[i] shr 8 and 0xFF
                val rb = result[i] and 0xFF
                result[i] = ((rr + pr) shl 16) or ((rg + pg) shl 8) or (rb + pb)
            }
        }

        val out = IntArray(w * h)
        for (i in result.indices) {
            val r = (result[i] shr 16 and 0xFF) / n
            val g = (result[i] shr 8 and 0xFF) / n
            val b = (result[i] and 0xFF) / n
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        bmp.setPixels(out, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun weightedMerge(frames: List<FrameData>): Bitmap {
        val w = frames.first().width
        val h = frames.first().height
        val weights = computeWeights(frames)
        val totalWeight = weights.sum()

        val accR = IntArray(w * h)
        val accG = IntArray(w * h)
        val accB = IntArray(w * h)

        for ((i, frame) in frames.withIndex()) {
            val pixels = IntArray(w * h)
            frame.bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            val wgt = weights[i]
            for (j in pixels.indices) {
                accR[j] += ((pixels[j] shr 16 and 0xFF) * wgt)
                accG[j] += ((pixels[j] shr 8 and 0xFF) * wgt)
                accB[j] += ((pixels[j] and 0xFF) * wgt)
            }
        }

        val out = IntArray(w * h)
        for (i in accR.indices) {
            val r = (accR[i] / totalWeight).coerceIn(0, 255)
            val g = (accG[i] / totalWeight).coerceIn(0, 255)
            val b = (accB[i] / totalWeight).coerceIn(0, 255)
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        bmp.setPixels(out, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun motionAwareMerge(frames: List<FrameData>): Bitmap {
        val startTime = System.currentTimeMillis()
        val w = frames.first().width
        val h = frames.first().height
        val n = frames.size

        val allPixels = frames.map { frame ->
            val pixels = IntArray(w * h)
            frame.bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            pixels
        }

        val refGray = luminance(allPixels[0], w, h)
        val noiseLevel = estimateNoiseLevel(refGray, w, h).coerceAtLeast(3f)
        val globalWeights = computeWeights(frames)

        val perPixelWeights = Array(n) { FloatArray(w * h) { 1f } }

        for (i in 1 until n) {
            val targetGray = luminance(allPixels[i], w, h)
            val diffMap = computeDiffMap(refGray, targetGray, w, h)
            val smoothedDiff = boxBlur5x5(diffMap, w, h)
            val motionWeights = diffToWeightMap(smoothedDiff, noiseLevel)
            val globalW = globalWeights[i].toFloat()
            val wgtArr = perPixelWeights[i]
            for (j in 0 until w * h) {
                wgtArr[j] = globalW * motionWeights[j]
            }
        }

        perPixelWeights[0] = FloatArray(w * h) { globalWeights[0].toFloat() }

        val resultPixels = IntArray(w * h)
        for (j in 0 until w * h) {
            var sumR = 0f
            var sumG = 0f
            var sumB = 0f
            var totalW = 0f
            for (i in 0 until n) {
                val px = allPixels[i][j]
                val wgt = perPixelWeights[i][j]
                sumR += (px shr 16 and 0xFF) * wgt
                sumG += (px shr 8 and 0xFF) * wgt
                sumB += (px and 0xFF) * wgt
                totalW += wgt
            }
            if (totalW > 0f) {
                val r = (sumR / totalW).toInt().coerceIn(0, 255)
                val g = (sumG / totalW).toInt().coerceIn(0, 255)
                val b = (sumB / totalW).toInt().coerceIn(0, 255)
                resultPixels[j] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            } else {
                resultPixels[j] = allPixels[0][j]
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Motion-aware merge took ${elapsed}ms (noise=$noiseLevel)")

        val bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        bmp.setPixels(resultPixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun luminance(pixels: IntArray, w: Int, h: Int): IntArray {
        val lum = IntArray(w * h)
        for (i in pixels.indices) {
            val r = pixels[i] shr 16 and 0xFF
            val g = pixels[i] shr 8 and 0xFF
            val b = pixels[i] and 0xFF
            lum[i] = (r * 77 + g * 150 + b * 29) shr 8
        }
        return lum
    }

    private fun computeDiffMap(
        refLum: IntArray,
        tgtLum: IntArray,
        w: Int,
        h: Int
    ): FloatArray {
        val diff = FloatArray(w * h)
        for (i in diff.indices) {
            diff[i] = abs(refLum[i] - tgtLum[i]).toFloat()
        }
        return diff
    }

    private fun boxBlur5x5(src: FloatArray, w: Int, h: Int): FloatArray {
        val tmp = FloatArray(w * h)
        val dst = FloatArray(w * h)

        for (y in 0 until h) {
            val rowBase = y * w
            for (x in 0 until w) {
                var sum = 0f
                var count = 0
                val x0 = (x - 2).coerceAtLeast(0)
                val x1 = (x + 2).coerceAtMost(w - 1)
                for (kx in x0..x1) {
                    sum += src[rowBase + kx]
                    count++
                }
                tmp[rowBase + x] = sum / count
            }
        }

        for (y in 0 until h) {
            val y0 = (y - 2).coerceAtLeast(0)
            val y1 = (y + 2).coerceAtMost(h - 1)
            val rangeRows = y1 - y0 + 1
            for (x in 0 until w) {
                var sum = 0f
                for (ky in y0..y1) {
                    sum += tmp[ky * w + x]
                }
                dst[y * w + x] = sum / rangeRows
            }
        }

        return dst
    }

    private fun diffToWeightMap(
        diff: FloatArray,
        noiseLevel: Float
    ): FloatArray {
        val low = noiseLevel * 1.5f
        val high = (noiseLevel * 5f).coerceAtLeast(low + 1f)
        val weights = FloatArray(diff.size)
        for (i in diff.indices) {
            val d = diff[i]
            if (d <= low) {
                weights[i] = 1.0f
            } else if (d >= high) {
                weights[i] = 0.0f
            } else {
                val t = (d - low) / (high - low)
                weights[i] = t * t * (3f - 2f * t)
            }
        }
        return weights
    }

    private fun estimateNoiseLevel(lum: IntArray, w: Int, h: Int): Float {
        val regionSize = 48
        val regions = listOf(
            0 to 0,
            w - regionSize to 0,
            0 to h - regionSize,
            w - regionSize to h - regionSize,
            (w - regionSize) / 2 to (h - regionSize) / 2
        )
        var bestNoise = Float.MAX_VALUE
        for ((rx, ry) in regions) {
            if (rx < 0 || ry < 0) continue
            val rh = regionSize.coerceAtMost(h - ry)
            val rw = regionSize.coerceAtMost(w - rx)
            if (rw < 8 || rh < 8) continue
            var sum = 0f
            var count = 0
            for (y in 1 until rh) {
                for (x in 1 until rw) {
                    val idx = (ry + y) * w + (rx + x)
                    val dx = abs(lum[idx] - lum[(ry + y) * w + (rx + x - 1)])
                    val dy = abs(lum[idx] - lum[(ry + y - 1) * w + (rx + x)])
                    sum += (dx + dy) * 0.5f
                    count++
                }
            }
            if (count > 0) {
                val noise = sum / count
                if (noise < bestNoise) bestNoise = noise
            }
        }
        return if (bestNoise < Float.MAX_VALUE) bestNoise else 10f
    }

    private fun computeWeights(frames: List<FrameData>): IntArray {
        val n = frames.size
        val sharpness = frames.map { computeSharpness(it.bitmap) }
        val maxSharpness = sharpness.maxOrNull()?.toFloat() ?: 1f
        return IntArray(n) { i ->
            val w = (sharpness[i] / maxSharpness * 100).toInt()
            w.coerceAtLeast(1)
        }
    }

    private fun computeSharpness(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        var sum = 0f
        var count = 0
        for (y in 1 until h) {
            for (x in 1 until w) {
                val px = pixels[y * w + x]
                val pl = pixels[y * w + (x - 1)]
                val pa = pixels[(y - 1) * w + x]

                val currLum = ((px shr 16 and 0xFF) * 77 +
                    (px shr 8 and 0xFF) * 150 +
                    (px and 0xFF) * 29) shr 8
                val leftLum = ((pl shr 16 and 0xFF) * 77 +
                    (pl shr 8 and 0xFF) * 150 +
                    (pl and 0xFF) * 29) shr 8
                val aboveLum = ((pa shr 16 and 0xFF) * 77 +
                    (pa shr 8 and 0xFF) * 150 +
                    (pa and 0xFF) * 29) shr 8

                val dx = absDiff(currLum, leftLum)
                val dy = absDiff(currLum, aboveLum)
                sum += dx + dy
                count++
            }
        }
        return if (count > 0) sum / count else 0f
    }

    private fun absDiff(a: Int, b: Int): Float = abs(a - b).toFloat()

    private fun estimateNoiseFloor(src: IntArray, w: Int, h: Int): Float {
        val step = 8
        var sumMin = 0f
        var count = 0
        for (y in step until h - step step step) {
            for (x in step until w - step step step) {
                val idx = y * w + x
                val l = ((src[idx] shr 16 and 0xFF) * 77 +
                    (src[idx] shr 8 and 0xFF) * 150 +
                    (src[idx] and 0xFF) * 29) shr 8
                val lx = ((src[idx - 1] shr 16 and 0xFF) * 77 +
                    (src[idx - 1] shr 8 and 0xFF) * 150 +
                    (src[idx - 1] and 0xFF) * 29) shr 8
                val ly = ((src[idx - w] shr 16 and 0xFF) * 77 +
                    (src[idx - w] shr 8 and 0xFF) * 150 +
                    (src[idx - w] and 0xFF) * 29) shr 8
                val dx = abs(l - lx).toFloat()
                val dy = abs(l - ly).toFloat()
                sumMin += minOf(dx, dy)
                count++
            }
        }
        return if (count > 0) (sumMin / count).coerceIn(1f, 40f) else 5f
    }

    fun preFilterFrame(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = src.copyOf()
        val radius = 1
        val area = (radius * 2 + 1) * (radius * 2 + 1)

        for (y in radius until h - radius) {
            for (x in radius until w - radius) {
                var sumR = 0; var sumG = 0; var sumB = 0
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val p = src[(y + dy) * w + (x + dx)]
                        sumR += p shr 16 and 0xFF
                        sumG += p shr 8 and 0xFF
                        sumB += p and 0xFF
                    }
                }
                val i = y * w + x
                dst[i] = (0xFF shl 24) or
                    ((sumR / area) shl 16) or
                    ((sumG / area) shl 8) or
                    (sumB / area)
            }
        }

        val result = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    fun enhanceDetail(bitmap: Bitmap, denoiseStrength: Float, sharpenStrength: Float): Bitmap {
        if (denoiseStrength <= 0f && sharpenStrength <= 0f) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = IntArray(w * h)

        val noiseFloor = estimateNoiseFloor(src, w, h)
        val noiseThreshold = 0.3f + noiseFloor * 0.008f

        val radius = 2
        val spatialSigma = 2.5f
        val spatialFactor = -0.5f / (spatialSigma * spatialSigma)

        val rangeSigma = 15f + denoiseStrength * 30f
        val rangeFactor = -1f / (rangeSigma * rangeSigma)

        val spatialWeights = FloatArray((2 * radius + 1) * (2 * radius + 1))
        var gaussianTotalW = 0f
        var wi = 0
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val sw = exp((dx * dx + dy * dy).toFloat() * spatialFactor)
                spatialWeights[wi++] = sw
                gaussianTotalW += sw
            }
        }

        for (y in radius until h - radius) {
            for (x in radius until w - radius) {
                val centerIdx = y * w + x
                val cp = src[centerIdx]
                val cr = cp shr 16 and 0xFF
                val cg = cp shr 8 and 0xFF
                val cb = cp and 0xFF

                var bilateralSumR = 0f
                var bilateralSumG = 0f
                var bilateralSumB = 0f
                var bilateralTotalW = 0f
                var gaussianSumR = 0f
                var gaussianSumG = 0f
                var gaussianSumB = 0f
                wi = 0

                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val p = src[(y + dy) * w + (x + dx)]
                        val pr = p shr 16 and 0xFF
                        val pg = p shr 8 and 0xFF
                        val pb = p and 0xFF

                        val spatialW = spatialWeights[wi++]

                        val dr = (pr - cr).toFloat()
                        val dg = (pg - cg).toFloat()
                        val db = (pb - cb).toFloat()
                        val rangeW = exp((dr * dr + dg * dg + db * db) * rangeFactor)
                        val bilateralW = spatialW * rangeW

                        bilateralSumR += pr * bilateralW
                        bilateralSumG += pg * bilateralW
                        bilateralSumB += pb * bilateralW
                        bilateralTotalW += bilateralW

                        gaussianSumR += pr * spatialW
                        gaussianSumG += pg * spatialW
                        gaussianSumB += pb * spatialW
                    }
                }

                val baseBilateralR = (bilateralSumR / bilateralTotalW).toInt().coerceIn(0, 255)
                val baseBilateralG = (bilateralSumG / bilateralTotalW).toInt().coerceIn(0, 255)
                val baseBilateralB = (bilateralSumB / bilateralTotalW).toInt().coerceIn(0, 255)

                val baseGaussianR = (gaussianSumR / gaussianTotalW).toInt().coerceIn(0, 255)
                val baseGaussianG = (gaussianSumG / gaussianTotalW).toInt().coerceIn(0, 255)
                val baseGaussianB = (gaussianSumB / gaussianTotalW).toInt().coerceIn(0, 255)

                val edgeActivity = 1f - (bilateralTotalW / gaussianTotalW)
                val adjustedEdgeActivity = (edgeActivity - noiseThreshold * 0.3f).coerceAtLeast(0f)
                val t = (adjustedEdgeActivity / 0.4f).coerceAtMost(1f)

                val denoiseGain = (1f - t) * denoiseStrength
                val sharpenGain = t * sharpenStrength

                val outR = (cr + denoiseGain * (baseBilateralR - cr) + sharpenGain * (cr - baseGaussianR))
                    .toInt().coerceIn(0, 255)
                val outG = (cg + denoiseGain * (baseBilateralG - cg) + sharpenGain * (cg - baseGaussianG))
                    .toInt().coerceIn(0, 255)
                val outB = (cb + denoiseGain * (baseBilateralB - cb) + sharpenGain * (cb - baseGaussianB))
                    .toInt().coerceIn(0, 255)

                dst[centerIdx] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        for (y in 0 until radius) {
            src.copyInto(dst, y * w, y * w, y * w + w)
        }
        for (y in h - radius until h) {
            src.copyInto(dst, y * w, y * w, y * w + w)
        }
        for (y in radius until h - radius) {
            for (x in 0 until radius) {
                dst[y * w + x] = src[y * w + x]
            }
            for (x in w - radius until w) {
                dst[y * w + x] = src[y * w + x]
            }
        }

        val result = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    fun gentleDenoise(bitmap: Bitmap, strength: Float): Bitmap {
        if (strength <= 0f) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = src.copyOf()

        val sigma = strength * 30f
        val sigmaInv2 = 1f / (sigma * sigma)

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val centerIdx = y * w + x
                val cp = src[centerIdx]
                val cr = cp shr 16 and 0xFF
                val cg = cp shr 8 and 0xFF
                val cb = cp and 0xFF

                var sumR = 0f; var sumG = 0f; var sumB = 0f; var totalW = 0f

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val p = src[(y + dy) * w + (x + dx)]
                        val pr = p shr 16 and 0xFF
                        val pg = p shr 8 and 0xFF
                        val pb = p and 0xFF

                        val dr = pr - cr
                        val dg = pg - cg
                        val db = pb - cb
                        val wgt = exp(-(dr * dr + dg * dg + db * db) * sigmaInv2)

                        sumR += pr * wgt
                        sumG += pg * wgt
                        sumB += pb * wgt
                        totalW += wgt
                    }
                }

                if (totalW > 0f) {
                    dst[centerIdx] = (0xFF shl 24) or
                        ((sumR / totalW).toInt().coerceIn(0, 255) shl 16) or
                        ((sumG / totalW).toInt().coerceIn(0, 255) shl 8) or
                        ((sumB / totalW).toInt().coerceIn(0, 255))
                }
            }
        }

        val result = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    fun lightSharpen(bitmap: Bitmap, strength: Float): Bitmap {
        if (strength <= 0f) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = src.copyOf()
        val radius = 1
        val kernelSize = radius * 2 + 1
        val area = kernelSize * kernelSize

        for (y in radius until h - radius) {
            for (x in radius until w - radius) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                for (ky in -radius..radius) {
                    for (kx in -radius..radius) {
                        val p = src[(y + ky) * w + (x + kx)]
                        sumR += p shr 16 and 0xFF
                        sumG += p shr 8 and 0xFF
                        sumB += p and 0xFF
                    }
                }
                val meanR = sumR / area
                val meanG = sumG / area
                val meanB = sumB / area

                val origP = src[y * w + x]
                val origR = origP shr 16 and 0xFF
                val origG = origP shr 8 and 0xFF
                val origB = origP and 0xFF

                val sharpR = (origR + (origR - meanR) * strength).toInt().coerceIn(0, 255)
                val sharpG = (origG + (origG - meanG) * strength).toInt().coerceIn(0, 255)
                val sharpB = (origB + (origB - meanB) * strength).toInt().coerceIn(0, 255)

                dst[y * w + x] = (0xFF shl 24) or (sharpR shl 16) or (sharpG shl 8) or sharpB
            }
        }

        val result = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    fun adjustContrast(bitmap: Bitmap, amount: Float): Bitmap {
        if (amount <= 0f) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = IntArray(w * h)

        for (i in src.indices) {
            val r = src[i] shr 16 and 0xFF
            val g = src[i] shr 8 and 0xFF
            val b = src[i] and 0xFF

            val nr = sCurve(r, amount)
            val ng = sCurve(g, amount)
            val nb = sCurve(b, amount)

            dst[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
        }

        val result = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    fun boostSaturation(bitmap: Bitmap, factor: Float): Bitmap {
        if (factor <= 1f || factor > 2f) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = IntArray(w * h)

        for (i in src.indices) {
            val r = src[i] shr 16 and 0xFF
            val g = src[i] shr 8 and 0xFF
            val b = src[i] and 0xFF

            val gray = (r * 77 + g * 150 + b * 29) shr 8

            val nr = ((r - gray) * factor + gray).toInt().coerceIn(0, 255)
            val ng = ((g - gray) * factor + gray).toInt().coerceIn(0, 255)
            val nb = ((b - gray) * factor + gray).toInt().coerceIn(0, 255)

            dst[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
        }

        val result = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    fun enhanceLocalContrast(bitmap: Bitmap, strength: Float = 0.3f): Bitmap {
        if (strength <= 0f) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)

        val lum = IntArray(w * h)
        for (i in src.indices) {
            val r = src[i] shr 16 and 0xFF
            val g = src[i] shr 8 and 0xFF
            val b = src[i] and 0xFF
            lum[i] = (r * 77 + g * 150 + b * 29) shr 8
        }

        val blurRadius = 3
        val blurredLum = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0
                var count = 0
                for (dy in -blurRadius..blurRadius) {
                    for (dx in -blurRadius..blurRadius) {
                        val ny = (y + dy).coerceIn(0, h - 1)
                        val nx = (x + dx).coerceIn(0, w - 1)
                        sum += lum[ny * w + nx]
                        count++
                    }
                }
                blurredLum[y * w + x] = sum / count
            }
        }

        val dst = IntArray(w * h)
        for (i in src.indices) {
            val r = src[i] shr 16 and 0xFF
            val g = src[i] shr 8 and 0xFF
            val b = src[i] and 0xFF
            val origLum = lum[i]
            val blurred = blurredLum[i]
            val detail = origLum - blurred
            val gain = strength * detail

            val nr = (r + gain).toInt().coerceIn(0, 255)
            val ng = (g + gain).toInt().coerceIn(0, 255)
            val nb = (b + gain).toInt().coerceIn(0, 255)

            dst[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
        }

        val result = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    private fun sCurve(v: Int, amount: Float): Int {
        val x = v / 255f
        val y = x + amount * x * (1f - x) * (x - 0.5f) * 4f
        return (y * 255f).toInt().coerceIn(0, 255)
    }
}
