package com.google.jetpackcamera.core.camera.mfs

import com.google.jetpackcamera.model.CaptureResolutionMode
import kotlin.math.min
import kotlin.math.sqrt

object MfsResolutionPolicy {

    fun resolveScale(
        sensorMp: Float,
        zoomFactor: Float,
        lightLevel: LightLevel,
        mode: CaptureResolutionMode
    ): Float {
        val targetMp = resolveTargetMp(sensorMp, zoomFactor, lightLevel, mode)
        val scale = sqrt(targetMp / sensorMp.coerceAtLeast(1f))
        return scale.coerceIn(0.25f, 1.0f)
    }

    private fun resolveTargetMp(
        sensorMp: Float,
        zoomFactor: Float,
        lightLevel: LightLevel,
        mode: CaptureResolutionMode
    ): Float {
        return when (mode) {
            CaptureResolutionMode.MAX -> minOf(sensorMp, 50f)
            CaptureResolutionMode.HIGH -> 12f
            CaptureResolutionMode.MEDIUM -> 6f
            CaptureResolutionMode.LOW -> 3f
            CaptureResolutionMode.AUTO -> {
                var target = 12f
                target -= when (lightLevel) {
                    LightLevel.VERY_LOW -> 6f
                    LightLevel.LOW -> 3f
                    else -> 0f
                }
                target += (sqrt(zoomFactor.coerceAtLeast(1f)) - 1f) * 3f
                val cap = if (sensorMp > 20f) 12f else sensorMp.coerceAtLeast(3f)
                target.coerceIn(3f, cap)
            }
        }
    }
}
