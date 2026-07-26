package com.google.jetpackcamera.core.camera.mfs

import com.google.jetpackcamera.core.camera.tuning.LookProfile
import com.google.jetpackcamera.model.CaptureResolutionMode
import kotlin.math.roundToLong
import kotlin.math.sqrt

enum class LightLevel { BRIGHT, NORMAL, LOW, VERY_LOW }

enum class MergeStrategy { AVERAGE, WEIGHTED, MOTION_AWARE }

data class MfsConfig(
    val frameCount: Int,
    val frameGapMs: Long,
    val mergeStrategy: MergeStrategy,
    val preFilterStrength: Float,
    val denoiseStrength: Float,
    val sharpenStrength: Float,
    val clarityStrength: Float = 0f,
    val microContrastBoost: Float = 0f,
    val processingScale: Float = 1.0f,
    val lookProfile: LookProfile? = null,
    val zoomRatio: Float = 1f
) {
    companion object {
        private const val REFERENCE_MP = 12f
        private const val BASE_FRAMES_BRIGHT = 5
        private const val BASE_FRAMES_NORMAL = 8
        private const val BASE_FRAMES_LOW = 12
        private const val BASE_FRAMES_VERY_LOW = 16
        private const val MAX_FRAMES = 16
        private const val MIN_FRAMES = 1
        private const val MIN_FRAME_GAP_MS = 20L
        private const val MAX_TOTAL_CAPTURE_MS = 3000L

        fun compute(
            sensorMP: Float,
            lightLevel: LightLevel,
            zoomFactor: Float,
            liveIso: Int? = null,
            resolutionMode: CaptureResolutionMode = CaptureResolutionMode.AUTO
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

            val denoiseStrength = computeDenoiseStrength(zoomFactor, lightLevel, liveIso)

            val sharpenStrength = computeSharpenStrength(zoomFactor, lightLevel, liveIso)

            val clarityStrength = computeClarityStrength(zoomFactor, lightLevel, liveIso)

            val microContrastBoost = computeMicroContrastBoost(zoomFactor)

            val processingScale = MfsResolutionPolicy.resolveScale(
                sensorMp = sensorMP,
                zoomFactor = zoomFactor,
                lightLevel = lightLevel,
                mode = resolutionMode
            )

            val frameGapMs = computeFrameGap(
                lightLevel = lightLevel,
                liveIso = liveIso,
                sensorMP = sensorMP,
                frameCount = zoomScaled
            )

            return MfsConfig(
                frameCount = zoomScaled,
                frameGapMs = frameGapMs,
                mergeStrategy = if (zoomScaled >= 3) MergeStrategy.MOTION_AWARE else MergeStrategy.WEIGHTED,
                preFilterStrength = preFilterStrength,
                denoiseStrength = denoiseStrength,
                sharpenStrength = sharpenStrength,
                clarityStrength = clarityStrength,
                microContrastBoost = microContrastBoost,
                processingScale = processingScale,
                zoomRatio = zoomFactor
            )
        }

        private fun computeFrameGap(
            lightLevel: LightLevel,
            liveIso: Int?,
            sensorMP: Float,
            frameCount: Int
        ): Long {
            if (frameCount <= 1) return 0L

            val baseGap = when (lightLevel) {
                LightLevel.BRIGHT -> 80L
                LightLevel.NORMAL -> 120L
                LightLevel.LOW -> 180L
                LightLevel.VERY_LOW -> 250L
            }

            val isoPenalty = when {
                liveIso == null -> 0L
                liveIso > 3200 -> 120L
                liveIso > 1600 -> 80L
                liveIso > 800 -> 40L
                else -> 0L
            }

            val mpPenalty = if (sensorMP > 12f) ((sensorMP / 12f - 1f) * 30).roundToLong() else 0L

            val rawGap = baseGap + isoPenalty + mpPenalty
            val maxGap = MAX_TOTAL_CAPTURE_MS / frameCount
            return rawGap.coerceIn(MIN_FRAME_GAP_MS, maxGap)
        }

        private fun computeDenoiseStrength(
            zoomFactor: Float,
            lightLevel: LightLevel,
            liveIso: Int?
        ): Float {
            var baseDenoise = when {
                liveIso != null && liveIso > 1600 -> 0.12f
                liveIso != null && liveIso > 800 -> 0.07f
                lightLevel == LightLevel.VERY_LOW -> 0.10f
                lightLevel == LightLevel.LOW -> 0.04f
                else -> 0.0f
            }

            val zoomNoiseBoost = ((sqrt(zoomFactor.coerceAtLeast(1f)) - 1f) * 0.03f)
                .coerceIn(0f, 0.08f)
            baseDenoise += zoomNoiseBoost

            return baseDenoise.coerceIn(0f, 0.3f)
        }

        private fun computeSharpenStrength(
            zoomFactor: Float,
            lightLevel: LightLevel,
            liveIso: Int?
        ): Float {
            val baseSharpen = 0.30f

            val zoomBoost = ((sqrt(zoomFactor.coerceAtLeast(1f)) - 1f) * 0.20f)
                .coerceIn(0f, 0.35f)
            var strength = baseSharpen + zoomBoost

            val noisePenalty = when {
                liveIso != null -> when {
                    liveIso > 3200 -> 0.25f
                    liveIso > 1600 -> 0.18f
                    liveIso > 800 -> 0.10f
                    liveIso > 400 -> 0.03f
                    else -> 0f
                }
                else -> when (lightLevel) {
                    LightLevel.VERY_LOW -> 0.22f
                    LightLevel.LOW -> 0.10f
                    else -> 0f
                }
            }
            strength -= noisePenalty

            return strength.coerceIn(0.10f, 0.70f)
        }

        private fun computeMicroContrastBoost(zoomFactor: Float): Float {
            return ((zoomFactor - 1f) * 0.04f).coerceIn(0f, 0.15f)
        }

        private fun computeClarityStrength(
            zoomFactor: Float,
            lightLevel: LightLevel,
            liveIso: Int?
        ): Float {
            var baseClarity = 0.50f

            val zoomBoost = ((sqrt(zoomFactor.coerceAtLeast(1f)) - 1f) * 0.10f)
                .coerceIn(0f, 0.18f)
            baseClarity += zoomBoost

            val noisePenalty = when {
                liveIso != null -> when {
                    liveIso > 3200 -> 0.25f
                    liveIso > 1600 -> 0.15f
                    liveIso > 800 -> 0.08f
                    else -> 0f
                }
                else -> when (lightLevel) {
                    LightLevel.VERY_LOW -> 0.22f
                    LightLevel.LOW -> 0.12f
                    else -> 0f
                }
            }
            baseClarity -= noisePenalty

            return baseClarity.coerceIn(0.10f, 0.80f)
        }
    }
}
