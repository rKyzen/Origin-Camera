package com.google.jetpackcamera.core.camera.mfs

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.PointF
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import android.util.SizeF

private const val TAG = "LensDistortionCorrector"

class LensDistortionCorrector(
    private val characteristics: CameraCharacteristics? = null
) {
    private val coefficients: FloatArray?
    private val opticalCenter: PointF?
    private val focalLengths: SizeF?

    init {
        if (characteristics != null) {
            coefficients = getDistortionCoefficients(characteristics)
            opticalCenter = getOpticalCenter(characteristics)
            focalLengths = getFocalLengths(characteristics)
        } else {
            coefficients = null
            opticalCenter = null
            focalLengths = null
        }
    }

    fun correct(bitmap: Bitmap): Bitmap {
        if (coefficients == null || opticalCenter == null || focalLengths == null) {
            return bitmap
        }
        if (coefficients.all { it == 0f }) {
            return bitmap
        }

        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = IntArray(w * h)

        val cx = opticalCenter.x
        val cy = opticalCenter.y
        val fx = focalLengths.width
        val fy = focalLengths.height

        if (fx <= 0f || fy <= 0f || cx <= 0f || cy <= 0f) {
            Log.w(TAG, "Invalid calibration data, skipping correction")
            return bitmap
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                val nx = (x - cx) / fx
                val ny = (y - cy) / fy

                val (dx, dy) = distortPoint(nx, ny, coefficients)

                val sx = dx * fx + cx
                val sy = dy * fy + cy

                if (sx < 0f || sx >= w - 1 || sy < 0f || sy >= h - 1) {
                    dst[y * w + x] = src[y * w + x]
                } else {
                    dst[y * w + x] = bilinearSample(src, w, h, sx, sy)
                }
            }
        }

        val result = Bitmap.createBitmap(w, h, Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    private fun distortPoint(nx: Float, ny: Float, coeffs: FloatArray): Pair<Float, Float> {
        val k1 = coeffs.getOrElse(0) { 0f }
        val k2 = coeffs.getOrElse(1) { 0f }
        val k3 = coeffs.getOrElse(2) { 0f }
        val k4 = coeffs.getOrElse(3) { 0f }
        val k5 = coeffs.getOrElse(4) { 0f }
        val k6 = coeffs.getOrElse(5) { 0f }
        val p1 = coeffs.getOrElse(6) { 0f }
        val p2 = coeffs.getOrElse(7) { 0f }

        val r2 = nx * nx + ny * ny
        val r4 = r2 * r2
        val r6 = r4 * r2

        val radial: Float
        if (k4 != 0f || k5 != 0f || k6 != 0f) {
            val denom = 1f + k4 * r2 + k5 * r4 + k6 * r6
            radial = (1f + k1 * r2 + k2 * r4 + k3 * r6) / denom
        } else {
            radial = 1f + k1 * r2 + k2 * r4 + k3 * r6
        }

        val dx = nx * radial + 2f * p1 * nx * ny + p2 * (r2 + 2f * nx * nx)
        val dy = ny * radial + p1 * (r2 + 2f * ny * ny) + 2f * p2 * nx * ny

        return dx to dy
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

    companion object {
        private const val DEFAULT_FX = 1f
        private const val DEFAULT_FY = 1f

        fun getDistortionCoefficients(characteristics: CameraCharacteristics): FloatArray? {
            val distortion = characteristics.get(CameraCharacteristics.LENS_DISTORTION)
            if (distortion != null && distortion.isNotEmpty()) {
                return distortion
            }
            val radial = characteristics.get(CameraCharacteristics.LENS_RADIAL_DISTORTION)
            if (radial != null && radial.isNotEmpty()) {
                return radial
            }
            return null
        }

        fun getOpticalCenter(characteristics: CameraCharacteristics): PointF? {
            val intrinsic = characteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
            if (intrinsic != null && intrinsic.size >= 2) {
                return PointF(intrinsic[2], intrinsic[3])
            }
            return null
        }

        fun getFocalLengths(characteristics: CameraCharacteristics): SizeF? {
            val intrinsic = characteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
            if (intrinsic != null && intrinsic.size >= 2) {
                return SizeF(intrinsic[0], intrinsic[1])
            }
            return null
        }
    }
}
