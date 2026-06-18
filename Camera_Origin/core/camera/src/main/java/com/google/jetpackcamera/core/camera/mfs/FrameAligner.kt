package com.google.jetpackcamera.core.camera.mfs

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class FrameAligner(
    private val blockSize: Int = 64,
    private val searchRadius: Int = 32,
    private val coarseStep: Int = 2
) {

    data class SimilarityModel(
        val a: Float,
        val b: Float,
        val tx: Float,
        val ty: Float,
        val confidence: Float
    ) {
        val scale: Float get() = sqrt(a * a + b * b)
        val rotationRad: Float get() = atan2(b, a)

        fun apply(x: Float, y: Float): Pair<Float, Float> =
            Pair(a * x - b * y + tx, b * x + a * y + ty)

        fun inverse(): SimilarityModel {
            val denom = a * a + b * b
            if (denom < 1e-10f) return SimilarityModel(1f, 0f, 0f, 0f, 0f)
            val aInv = a / denom
            val bInv = -b / denom
            val txInv = (-a * tx - b * ty) / denom
            val tyInv = (b * tx - a * ty) / denom
            return SimilarityModel(aInv, bInv, txInv, tyInv, confidence)
        }

        fun isSignificant(epsTx: Float = 0.5f, epsRot: Float = 0.005f, epsScale: Float = 1e-3f): Boolean =
            abs(tx) > epsTx || abs(ty) > epsTx ||
                abs(rotationRad) > epsRot || abs(scale - 1f) > epsScale
    }

    fun alignFrames(
        reference: FrameData,
        targets: List<FrameData>
    ): List<FrameData> {
        val refGray = toGray(reference.bitmap)
        val refWidth = reference.width
        val refHeight = reference.height

        return targets.map { target ->
            val model = estimateTransform(refGray, refWidth, refHeight, target)
            if (model.confidence > 0.2f && model.isSignificant()) {
                val warped = warpSimilarity(target.bitmap, model)
                target.copy(bitmap = warped)
            } else {
                target
            }
        }
    }

    private data class BlockDisplacement(
        val centerX: Int,
        val centerY: Int,
        val dx: Float,
        val dy: Float
    )

    private fun estimateTransform(
        refGray: IntArray,
        refWidth: Int,
        refHeight: Int,
        target: FrameData
    ): SimilarityModel {
        val targetGray = toGray(target.bitmap)
        val numBlocksX = (refWidth / blockSize).coerceAtLeast(1)
        val numBlocksY = (refHeight / blockSize).coerceAtLeast(1)
        val totalBlocks = numBlocksX * numBlocksY

        var totalDx = 0f
        var totalDy = 0f
        var validBlocks = 0
        val adjustSearch = min(searchRadius, min(refWidth, refHeight) / 20)
        val displacements = mutableListOf<BlockDisplacement>()

        for (by in 0 until numBlocksY) {
            for (bx in 0 until numBlocksX) {
                val blockX = bx * blockSize
                val blockY = by * blockSize
                val bw = min(blockSize, refWidth - blockX)
                val bh = min(blockSize, refHeight - blockY)
                if (bw < 16 || bh < 16) continue

                val result = findTranslationInBlock(
                    refGray, refWidth, refHeight,
                    targetGray, target.width, target.height,
                    blockX, blockY, bw, bh, adjustSearch
                )
                if (result.confidence > 0.3f) {
                    displacements.add(
                        BlockDisplacement(
                            centerX = blockX + bw / 2,
                            centerY = blockY + bh / 2,
                            dx = result.offsetX,
                            dy = result.offsetY
                        )
                    )
                    totalDx += result.offsetX
                    totalDy += result.offsetY
                    validBlocks++
                }
            }
        }

        if (validBlocks == 0) {
            return SimilarityModel(1f, 0f, 0f, 0f, 0f)
        }

        if (validBlocks >= 3) {
            val ransacModel = ransacSimilarity(displacements, totalBlocks)
            if (ransacModel.confidence > 0.1f) {
                return ransacModel
            }
        }

        val avgDx = totalDx / validBlocks
        val avgDy = totalDy / validBlocks
        val confidence = validBlocks.toFloat() / totalBlocks
        return SimilarityModel(1f, 0f, avgDx, avgDy, confidence)
    }

    private fun ransacSimilarity(
        points: List<BlockDisplacement>,
        totalBlocks: Int,
        iterations: Int = 50,
        inlierThreshold: Float = 2.5f
    ): SimilarityModel {
        if (points.size < 2) {
            return SimilarityModel(1f, 0f, 0f, 0f, points.size.toFloat() / totalBlocks)
        }

        var bestInliers = emptyList<BlockDisplacement>()
        val rng = Random

        repeat(iterations) {
            val idx1 = rng.nextInt(points.size)
            var idx2 = rng.nextInt(points.size)
            while (idx2 == idx1) {
                idx2 = rng.nextInt(points.size)
            }

            val model = solveSimilarityFromTwoPoints(points[idx1], points[idx2]) ?: return@repeat

            val inliers = points.filter { p ->
                val (ex, ey) = model.apply(p.centerX.toFloat(), p.centerY.toFloat())
                val errX = ex - (p.centerX + p.dx)
                val errY = ey - (p.centerY + p.dy)
                errX * errX + errY * errY <= inlierThreshold * inlierThreshold
            }

            if (inliers.size > bestInliers.size) {
                bestInliers = inliers
            }
        }

        if (bestInliers.size < 2) {
            return SimilarityModel(1f, 0f, 0f, 0f, 0f)
        }

        val model = fitSimilarityLeastSquares(bestInliers)
        val confidence = bestInliers.size.toFloat() / totalBlocks
        return model.copy(confidence = confidence)
    }

    private fun solveSimilarityFromTwoPoints(
        p1: BlockDisplacement,
        p2: BlockDisplacement
    ): SimilarityModel? {
        val dx = (p2.centerX - p1.centerX).toFloat()
        val dy = (p2.centerY - p1.centerY).toFloat()
        val ddx = p2.dx - p1.dx
        val ddy = p2.dy - p1.dy

        val det = dx * dx + dy * dy
        if (det < 1f) return null

        val A = (dx * ddx + dy * ddy) / det
        val B = (dx * ddy - dy * ddx) / det

        val a = A + 1f
        val b = B
        val tx = p1.dx - A * p1.centerX + B * p1.centerY
        val ty = p1.dy - B * p1.centerX - A * p1.centerY

        return SimilarityModel(a, b, tx, ty, 0f)
    }

    private fun fitSimilarityLeastSquares(points: List<BlockDisplacement>): SimilarityModel {
        var sumX = 0f
        var sumY = 0f
        var sumDx = 0f
        var sumDy = 0f
        var sumXcDx = 0f
        var sumYcDy = 0f
        var sumXcDy = 0f
        var sumYcDx = 0f
        var sumXcXc = 0f
        var sumYcYc = 0f

        val n = points.size.toFloat()
        for (p in points) {
            sumX += p.centerX.toFloat()
            sumY += p.centerY.toFloat()
            sumDx += p.dx
            sumDy += p.dy
        }
        val cx = sumX / n
        val cy = sumY / n

        for (p in points) {
            val xc = p.centerX.toFloat() - cx
            val yc = p.centerY.toFloat() - cy
            sumXcDx += xc * p.dx
            sumYcDy += yc * p.dy
            sumXcDy += xc * p.dy
            sumYcDx += yc * p.dx
            sumXcXc += xc * xc
            sumYcYc += yc * yc
        }

        val denom = sumXcXc + sumYcYc
        val A = if (denom > 0f) (sumXcDx + sumYcDy) / denom else 0f
        val B = if (denom > 0f) (sumXcDy - sumYcDx) / denom else 0f

        val a = A + 1f
        val b = B
        val tx = sumDx / n - A * cx + B * cy
        val ty = sumDy / n - B * cx - A * cy

        return SimilarityModel(a, b, tx, ty, 0f)
    }

    private fun findTranslationInBlock(
        refGray: IntArray,
        refWidth: Int,
        refHeight: Int,
        targetGray: IntArray,
        targetWidth: Int,
        targetHeight: Int,
        blockX: Int,
        blockY: Int,
        blockW: Int,
        blockH: Int,
        searchRadius: Int
    ): AlignmentResult {
        var bestDx = 0
        var bestDy = 0
        var bestMse = Float.MAX_VALUE

        for (dy in -searchRadius..searchRadius step coarseStep) {
            for (dx in -searchRadius..searchRadius step coarseStep) {
                val mse = computeBlockMse(
                    refGray, refWidth, refHeight,
                    targetGray, targetWidth, targetHeight,
                    blockX, blockY, blockW, blockH,
                    dx, dy
                )
                if (mse < bestMse) {
                    bestMse = mse
                    bestDx = dx
                    bestDy = dy
                }
            }
        }

        val fineStartDx = (bestDx - coarseStep + 1).coerceAtLeast(-searchRadius)
        val fineEndDx = (bestDx + coarseStep - 1).coerceAtMost(searchRadius)
        val fineStartDy = (bestDy - coarseStep + 1).coerceAtLeast(-searchRadius)
        val fineEndDy = (bestDy + coarseStep - 1).coerceAtMost(searchRadius)
        for (dy in fineStartDy..fineEndDy) {
            for (dx in fineStartDx..fineEndDx) {
                val mse = computeBlockMse(
                    refGray, refWidth, refHeight,
                    targetGray, targetWidth, targetHeight,
                    blockX, blockY, blockW, blockH,
                    dx, dy
                )
                if (mse < bestMse) {
                    bestMse = mse
                    bestDx = dx
                    bestDy = dy
                }
            }
        }

        val subPixelOffsetX = refineSubPixelX(
            refGray, refWidth, refHeight,
            targetGray, targetWidth, targetHeight,
            blockX, blockY, blockW, blockH,
            bestDx, bestDy
        )
        val subPixelOffsetY = refineSubPixelY(
            refGray, refWidth, refHeight,
            targetGray, targetWidth, targetHeight,
            blockX, blockY, blockW, blockH,
            bestDx, bestDy
        )

        val confidence = 1f / (1f + bestMse / 1000f)
        return AlignmentResult(
            bestDx + subPixelOffsetX,
            bestDy + subPixelOffsetY,
            confidence
        )
    }

    private fun refineSubPixelX(
        refGray: IntArray, refW: Int, refH: Int,
        targetGray: IntArray, targetW: Int, targetH: Int,
        bx: Int, by: Int, bw: Int, bh: Int,
        bestDx: Int, bestDy: Int
    ): Float {
        val m0 = computeBlockMse(refGray, refW, refH, targetGray, targetW, targetH, bx, by, bw, bh, bestDx - 1, bestDy)
        val m1 = computeBlockMse(refGray, refW, refH, targetGray, targetW, targetH, bx, by, bw, bh, bestDx, bestDy)
        val m2 = computeBlockMse(refGray, refW, refH, targetGray, targetW, targetH, bx, by, bw, bh, bestDx + 1, bestDy)
        if (m0 >= m1 || m2 >= m1) return 0f
        return 0.5f * (m0 - m2) / (m0 - 2f * m1 + m2)
    }

    private fun refineSubPixelY(
        refGray: IntArray, refW: Int, refH: Int,
        targetGray: IntArray, targetW: Int, targetH: Int,
        bx: Int, by: Int, bw: Int, bh: Int,
        bestDx: Int, bestDy: Int
    ): Float {
        val m0 = computeBlockMse(refGray, refW, refH, targetGray, targetW, targetH, bx, by, bw, bh, bestDx, bestDy - 1)
        val m1 = computeBlockMse(refGray, refW, refH, targetGray, targetW, targetH, bx, by, bw, bh, bestDx, bestDy)
        val m2 = computeBlockMse(refGray, refW, refH, targetGray, targetW, targetH, bx, by, bw, bh, bestDx, bestDy + 1)
        if (m0 >= m1 || m2 >= m1) return 0f
        return 0.5f * (m0 - m2) / (m0 - 2f * m1 + m2)
    }

    private fun computeBlockMse(
        refGray: IntArray, refW: Int, refH: Int,
        targetGray: IntArray, targetW: Int, targetH: Int,
        bx: Int, by: Int, bw: Int, bh: Int,
        dx: Int, dy: Int
    ): Float {
        val startX = bx.coerceAtLeast(if (dx < 0) -dx else 0)
        val startY = by.coerceAtLeast(if (dy < 0) -dy else 0)
        val endX = (bx + bw).coerceAtMost(refW)
            .coerceAtMost(if (dx >= 0) targetW - dx else targetW)
        val endY = (by + bh).coerceAtMost(refH)
            .coerceAtMost(if (dy >= 0) targetH - dy else targetH)

        if (endX <= startX || endY <= startY) return Float.MAX_VALUE

        var totalError = 0f
        var count = 0
        for (y in startY until endY) {
            val refRow = y * refW
            val targetRow = (y + dy) * targetW
            for (x in startX until endX) {
                val diff = refGray[refRow + x] - targetGray[targetRow + x + dx]
                totalError += diff * diff
                count++
            }
        }
        return if (count > 0) totalError / count else Float.MAX_VALUE
    }

    private data class AlignmentResult(
        val offsetX: Float,
        val offsetY: Float,
        val confidence: Float
    )

    private fun warpSimilarity(source: Bitmap, model: SimilarityModel): Bitmap {
        val w = source.width
        val h = source.height
        val src = IntArray(w * h)
        source.getPixels(src, 0, w, 0, 0, w, h)
        val dst = IntArray(w * h)

        val inv = model.inverse()

        for (y in 0 until h) {
            for (x in 0 until w) {
                val sx = inv.a * x - inv.b * y + inv.tx
                val sy = inv.b * x + inv.a * y + inv.ty
                dst[y * w + x] = bilinearSample(src, w, h, sx, sy)
            }
        }

        val result = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    private fun bilinearSample(pixels: IntArray, w: Int, h: Int, sx: Float, sy: Float): Int {
        val x0 = sx.toInt().coerceIn(0, w - 1)
        val y0 = sy.toInt().coerceIn(0, h - 1)
        val x1 = (x0 + 1).coerceAtMost(w - 1)
        val y1 = (y0 + 1).coerceAtMost(h - 1)

        val fx = sx - x0
        val fy = sy - y0

        val p00 = pixels[y0 * w + x0]
        val p10 = pixels[y0 * w + x1]
        val p01 = pixels[y1 * w + x0]
        val p11 = pixels[y1 * w + x1]

        val r = interpolate(
            (p00 shr 16 and 0xFF).toFloat(),
            (p10 shr 16 and 0xFF).toFloat(),
            (p01 shr 16 and 0xFF).toFloat(),
            (p11 shr 16 and 0xFF).toFloat(),
            fx, fy
        ).toInt().coerceIn(0, 255)
        val g = interpolate(
            (p00 shr 8 and 0xFF).toFloat(),
            (p10 shr 8 and 0xFF).toFloat(),
            (p01 shr 8 and 0xFF).toFloat(),
            (p11 shr 8 and 0xFF).toFloat(),
            fx, fy
        ).toInt().coerceIn(0, 255)
        val b = interpolate(
            (p00 and 0xFF).toFloat(),
            (p10 and 0xFF).toFloat(),
            (p01 and 0xFF).toFloat(),
            (p11 and 0xFF).toFloat(),
            fx, fy
        ).toInt().coerceIn(0, 255)

        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun interpolate(c00: Float, c10: Float, c01: Float, c11: Float, fx: Float, fy: Float): Float {
        val top = c00 + (c10 - c00) * fx
        val bottom = c01 + (c11 - c01) * fx
        return top + (bottom - top) * fy
    }

    private fun toGray(bitmap: Bitmap): IntArray {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val gray = IntArray(w * h)
        for (i in pixels.indices) {
            val r = pixels[i] shr 16 and 0xFF
            val g = pixels[i] shr 8 and 0xFF
            val b = pixels[i] and 0xFF
            gray[i] = (r * 77 + g * 150 + b * 29) shr 8
        }
        return gray
    }
}
