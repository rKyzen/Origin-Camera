package com.google.jetpackcamera.core.camera.tuning

import kotlin.math.sqrt

object LookProfileMapper {

    private const val MIN_BRIGHTNESS_FLOOR = 0.12f
    private const val MAX_SATURATION = 1.6f
    private const val MIN_SATURATION = 1.0f
    private const val MAX_CONTRAST = 0.25f
    private const val MIN_CONTRAST = 0.05f

    fun fromBalancedProfile(profile: BalancedProfile): LookProfile {
        val sceneType = inferSceneType(profile)
        return when (sceneType) {
            SceneType.DAY -> mapDayScene(profile)
            SceneType.NIGHT -> mapNightScene(profile)
            SceneType.NORMAL -> mapNormalScene(profile)
        }
    }

    fun default(): LookProfile = LookProfile.DEFAULT

    private fun inferSceneType(profile: BalancedProfile): SceneType {
        val brightness = profile.targetBrightness
        val noise = profile.targetNoise
        return when {
            brightness > 0.6f && noise < 0.03f -> SceneType.DAY
            brightness < 0.3f || noise > 0.06f -> SceneType.NIGHT
            else -> SceneType.NORMAL
        }
    }

    private fun mapDayScene(profile: BalancedProfile): LookProfile {
        val contrast = mapContrast(profile.targetContrast, bias = 0.3f)
        val saturation = mapSaturation(profile.targetSaturation, bias = 0.2f)
        val sharpness = mapSharpness(profile.targetSharpness)
        val noiseReduction = mapNoiseReduction(profile.targetNoise, aggressive = false)
        val brightness = mapBrightness(profile.targetBrightness, floor = MIN_BRIGHTNESS_FLOOR)
        return LookProfile(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            sharpness = sharpness,
            noiseReduction = noiseReduction
        )
    }

    private fun mapNightScene(profile: BalancedProfile): LookProfile {
        val contrast = mapContrast(profile.targetContrast, bias = 0.5f)
        val saturation = mapSaturation(profile.targetSaturation, bias = 0.4f)
        val sharpness = mapSharpness(profile.targetSharpness)
        val noiseReduction = mapNoiseReduction(profile.targetNoise, aggressive = true)
        val brightness = mapBrightness(profile.targetBrightness, floor = MIN_BRIGHTNESS_FLOOR + 0.05f)
        return LookProfile(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            sharpness = sharpness,
            noiseReduction = noiseReduction
        )
    }

    private fun mapNormalScene(profile: BalancedProfile): LookProfile {
        val contrast = mapContrast(profile.targetContrast, bias = 0.4f)
        val saturation = mapSaturation(profile.targetSaturation, bias = 0.3f)
        val sharpness = mapSharpness(profile.targetSharpness)
        val noiseReduction = mapNoiseReduction(profile.targetNoise, aggressive = false)
        val brightness = mapBrightness(profile.targetBrightness, floor = MIN_BRIGHTNESS_FLOOR)
        return LookProfile(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            sharpness = sharpness,
            noiseReduction = noiseReduction
        )
    }

    private fun mapContrast(targetContrast: Float, bias: Float): Float {
        val interpolated = targetContrast * (1f - bias) + 0.12f * bias
        return interpolated.coerceIn(MIN_CONTRAST, MAX_CONTRAST)
    }

    private fun mapSaturation(targetSaturation: Float, bias: Float): Float {
        val rawFactor = 1f + targetSaturation
        val interpolated = rawFactor * (1f - bias) + 1.18f * bias
        return interpolated.coerceIn(MIN_SATURATION, MAX_SATURATION)
    }

    private fun mapSharpness(targetSharpness: Float): Float {
        return targetSharpness.coerceIn(0.1f, 0.9f)
    }

    private fun mapNoiseReduction(targetNoise: Float, aggressive: Boolean): Float {
        val base = if (aggressive) {
            (sqrt(targetNoise) * 2f).coerceIn(0.05f, 0.4f)
        } else {
            (sqrt(targetNoise) * 1.2f).coerceIn(0.0f, 0.25f)
        }
        return base
    }

    private fun mapBrightness(targetBrightness: Float, floor: Float): Float {
        return targetBrightness.coerceAtLeast(floor).coerceIn(0f, 1f)
    }

    private enum class SceneType { DAY, NIGHT, NORMAL }
}
