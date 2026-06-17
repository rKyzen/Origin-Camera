package com.google.jetpackcamera.core.camera.mfs

import kotlin.math.sqrt

enum class LightLevel { BRIGHT, NORMAL, LOW, VERY_LOW }

enum class MergeStrategy { AVERAGE, WEIGHTED, MOTION_AWARE }

data class MfsConfig(
    val frameCount: Int,
    val frameGapMs: Long,
    val mergeStrategy: MergeStrategy,
    val preFilterStrength: Float,
    val denoiseStrength: Float,
    val sharpenStrength: Float
) {
    companion object {
        private const val REFERENCE_MP = 12f
        private const val BASE_FRAMES_BRIGHT = 5
        private const val BASE_FRAMES_NORMAL = 8
        private const val BASE_FRAMES_LOW = 12
        private const val BASE_FRAMES_VERY_LOW = 16
        private const val MAX_FRAMES = 16
        private const val MIN_FRAMES = 1

        fun compute(
            sensorMP: Float,
            lightLevel: LightLevel,
            zoomFactor: Float,
            liveIso: Int? = null
        ): MfsConfig {
            val baseFrames = when (lightLevel) {
                LightLevel.BRIGHT -> BASE_FRAMES_BRIGHT
                LightLevel.NORMAL -> BASE_FRAMES_NORMAL
                LightLevel.LOW -> BASE_FRAMES_LOW
                LightLevel.VERY_LOW -> BASE_FRAMES_VERY_LOW
            }

            val isoBoost = when {
                liveIso == null -> 1f
                liveIso > 1600 -> 1.5f
                liveIso > 800 -> 1.3f
                liveIso > 400 -> 1.15f
                else -> 1f
            }

            val mpScaled = (baseFrames * (REFERENCE_MP / sensorMP) * isoBoost).toInt()
                .coerceIn(MIN_FRAMES, MAX_FRAMES)

            val zoomScaled = (mpScaled * sqrt(zoomFactor.coerceAtLeast(1f))).toInt()
                .coerceIn(MIN_FRAMES, MAX_FRAMES)

            val preFilterStrength = when {
                liveIso != null && liveIso > 1600 -> 1.0f
                liveIso != null && liveIso > 800 -> 0.5f
                lightLevel == LightLevel.VERY_LOW -> 1.0f
                lightLevel == LightLevel.LOW -> 0.5f
                else -> 0.0f
            }

            val denoiseStrength = when {
                liveIso != null && liveIso > 1600 -> 0.3f
                liveIso != null && liveIso > 800 -> 0.15f
                lightLevel == LightLevel.VERY_LOW -> 0.25f
                lightLevel == LightLevel.LOW -> 0.1f
                else -> 0.0f
            }

            val sharpenStrength = when {
                zoomFactor > 5f -> 0.8f
                zoomFactor > 3f -> 0.7f
                lightLevel == LightLevel.VERY_LOW -> 0.4f
                else -> 0.5f
            }

            return MfsConfig(
                frameCount = zoomScaled,
                frameGapMs = 0L,
                mergeStrategy = if (zoomScaled >= 3) MergeStrategy.MOTION_AWARE else MergeStrategy.WEIGHTED,
                preFilterStrength = preFilterStrength,
                denoiseStrength = denoiseStrength,
                sharpenStrength = sharpenStrength
            )
        }
    }
}
