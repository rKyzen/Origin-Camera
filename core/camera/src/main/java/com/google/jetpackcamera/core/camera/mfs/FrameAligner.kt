package com.google.jetpackcamera.core.camera.mfs

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import kotlin.math.min

class FrameAligner(
    private val blockSize: Int = 64,
    private val searchRadius: Int = 32,
    private val coarseStep: Int = 2
) {

    data class AlignmentResult(
        val offsetX: Float,
        val offsetY: Float,
        val confidence: Float
    )

    fun alignFrames(
        reference: FrameData,
        targets: List<FrameData>
    ): List<FrameData> {
        val refGray = toGray(reference.bitmap)
        val refWidth = reference.width
        val refHeight = reference.height

        return targets.map { target ->
            val offset = findTranslationBlockBased(refGray, refWidth, refHeight, target)
            if (offset.confidence > 0.2f && (offset.offsetX != 0f || offset.offsetY != 0f)) {
                val shifted = shiftBitmapSubPixel(target.bitmap, offset.offsetX, offset.offsetY)
                target.copy(bitmap = shifted)
            } else {
                target
            }
        }
    }

    private fun findTranslationBlockBased(
        refGray: IntArray,
        refWidth: Int,
        refHeight: Int,
        target: FrameData
    ): AlignmentResult {
        val targetGray = toGray(target.bitmap)
        val numBlocksX = (refWidth / blockSize).coerceAtLeast(1)
        val numBlocksY = (refHeight / blockSize).coerceAtLeast(1)

        var totalDx = 0f
        var totalDy = 0f
        var validBlocks = 0
        val adjustSearch = min(searchRadius, min(refWidth, refHeight) / 20)

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
                    totalDx += result.offsetX
                    totalDy += result.offsetY
                    validBlocks++
                }
            }
        }

        if (validBlocks == 0) return AlignmentResult(0f, 0f, 0f)

        val avgDx = totalDx / validBlocks
        val avgDy = totalDy / validBlocks
        val confidence = (validBlocks.toFloat() / (numBlocksX * numBlocksY))
        return AlignmentResult(avgDx, avgDy, confidence)
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
        val startX = bx.coerceAtLeast(if (dx > 0) bx + dx else bx)
        val startY = by.coerceAtLeast(if (dy > 0) by + dy else by)
        val endX = (bx + bw).coerceAtMost(
            if (dx < 0) refW + dx else refW
        ).coerceAtMost(if (dx > 0) targetW else targetW - dx)
        val endY = (by + bh).coerceAtMost(
            if (dy < 0) refH + dy else refH
        ).coerceAtMost(if (dy > 0) targetH else targetH - dy)

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

    private fun shiftBitmapSubPixel(source: Bitmap, dx: Float, dy: Float): Bitmap {
        val w = source.width
        val h = source.height
        val src = IntArray(w * h)
        source.getPixels(src, 0, w, 0, 0, w, h)
        val dst = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val sx = x - dx
                val sy = y - dy
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
