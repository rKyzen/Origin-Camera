package com.google.jetpackcamera.core.camera.tuning

data class LookProfile(
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val sharpness: Float,
    val noiseReduction: Float
) {
    companion object {
        val DEFAULT = LookProfile(
            brightness = 0.55f,
            contrast = 0.14f,
            saturation = 1.22f,
            sharpness = 0.45f,
            noiseReduction = 0.1f
        )

        val NEUTRAL = LookProfile(
            brightness = 0.50f,
            contrast = 0.12f,
            saturation = 1.18f,
            sharpness = 0.40f,
            noiseReduction = 0.0f
        )
    }
}
