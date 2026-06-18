package com.google.jetpackcamera.core.camera.tuning

data class SceneReport(
    val sceneName: String,
    val stockMetrics: ImageMetrics,
    val cameraMetrics: ImageMetrics,
    val differences: MetricDifferences,
    val recommendations: List<Recommendation>,
    val balancedProfile: BalancedProfile
)

data class DatasetReport(
    val sceneReports: List<SceneReport>,
    val overallRecommendations: List<Recommendation>,
    val averageDifferences: MetricDifferences,
    val summary: String
)

data class MetricDifferences(
    val luminanceDiffPercent: Float,
    val contrastDiffPercent: Float,
    val saturationDiffPercent: Float,
    val sharpnessDiffPercent: Float,
    val noiseDiffPercent: Float,
    val brightnessDiffPercent: Float
)

data class Recommendation(
    val metric: String,
    val description: String,
    val suggestedDirection: Direction,
    val magnitude: Float,
    val priority: Priority
)

enum class Direction { INCREASE, DECREASE, BALANCE }
enum class Priority { HIGH, MEDIUM, LOW }

data class BalancedProfile(
    val targetLuminance: Float,
    val targetContrast: Float,
    val targetSaturation: Float,
    val targetSharpness: Float,
    val targetNoise: Float,
    val targetBrightness: Float
)
