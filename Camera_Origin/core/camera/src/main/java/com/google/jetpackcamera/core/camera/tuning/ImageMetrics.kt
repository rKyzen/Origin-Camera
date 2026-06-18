package com.google.jetpackcamera.core.camera.tuning

data class ImageMetrics(
    val luminance: LuminanceMetrics,
    val contrast: ContrastMetrics,
    val saturation: SaturationMetrics,
    val sharpness: SharpnessMetrics,
    val noise: NoiseMetrics,
    val perceivedBrightness: Float
)

data class LuminanceMetrics(
    val mean: Float,
    val median: Float,
    val standardDeviation: Float,
    val histogram: IntArray,
    val percentile1: Float,
    val percentile5: Float,
    val percentile50: Float,
    val percentile95: Float,
    val percentile99: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LuminanceMetrics) return false
        return mean == other.mean && median == other.median &&
            standardDeviation == other.standardDeviation &&
            histogram.contentEquals(other.histogram)
    }

    override fun hashCode(): Int {
        var result = mean.hashCode()
        result = 31 * result + median.hashCode()
        result = 31 * result + standardDeviation.hashCode()
        result = 31 * result + histogram.contentHashCode()
        return result
    }
}

data class ContrastMetrics(
    val rmsContrast: Float,
    val michelsonContrast: Float,
    val localContrast: Float
)

data class SaturationMetrics(
    val meanSaturation: Float,
    val maxSaturation: Float,
    val vibrance: Float
)

data class SharpnessMetrics(
    val laplacianVariance: Float,
    val sobelMagnitude: Float
)

data class NoiseMetrics(
    val estimatedNoise: Float,
    val signalToNoiseRatio: Float
)
