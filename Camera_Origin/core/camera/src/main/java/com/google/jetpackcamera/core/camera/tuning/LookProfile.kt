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
            contrast = 0.18f,
            saturation = 1.30f,
            sharpness = 0.45f,
            noiseReduction = 0.05f
        )

        val NEUTRAL = LookProfile(
            brightness = 0.52f,
            contrast = 0.14f,
            saturation = 1.20f,
            sharpness = 0.40f,
            noiseReduction = 0.0f
        )
    }
}
