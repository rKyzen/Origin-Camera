package com.google.jetpackcamera.core.camera.tuning

import kotlin.math.abs

object SceneComparison {

    private const val SIGNIFICANT_DIFF_THRESHOLD = 5f
    private const val LARGE_DIFF_THRESHOLD = 15f

    fun compare(sceneName: String, stockMetrics: ImageMetrics, cameraMetrics: ImageMetrics): SceneReport {
        val differences = computeDifferences(stockMetrics, cameraMetrics)
        val recommendations = generateRecommendations(differences, stockMetrics, cameraMetrics)
        val balancedProfile = BalancedProfileCalculator.calculate(stockMetrics, cameraMetrics)
        return SceneReport(
            sceneName = sceneName,
            stockMetrics = stockMetrics,
            cameraMetrics = cameraMetrics,
            differences = differences,
            recommendations = recommendations,
            balancedProfile = balancedProfile
        )
    }

    private fun computeDifferences(stock: ImageMetrics, camera: ImageMetrics): MetricDifferences {
        return MetricDifferences(
            luminanceDiffPercent = percentDiff(stock.luminance.mean, camera.luminance.mean),
            contrastDiffPercent = percentDiff(stock.contrast.rmsContrast, camera.contrast.rmsContrast),
            saturationDiffPercent = percentDiff(stock.saturation.meanSaturation, camera.saturation.meanSaturation),
            sharpnessDiffPercent = percentDiff(stock.sharpness.laplacianVariance, camera.sharpness.laplacianVariance),
            noiseDiffPercent = percentDiff(stock.noise.estimatedNoise, camera.noise.estimatedNoise),
            brightnessDiffPercent = percentDiff(stock.perceivedBrightness, camera.perceivedBrightness)
        )
    }

    private fun generateRecommendations(
        diffs: MetricDifferences,
        stock: ImageMetrics,
        camera: ImageMetrics
    ): List<Recommendation> {
        val recs = mutableListOf<Recommendation>()

        if (abs(diffs.brightnessDiffPercent) > SIGNIFICANT_DIFF_THRESHOLD) {
            val direction = if (diffs.brightnessDiffPercent > 0) Direction.INCREASE else Direction.DECREASE
            recs.add(
                Recommendation(
                    metric = "Brightness",
                    description = describeBrightnessDiff(diffs.brightnessDiffPercent, stock, camera),
                    suggestedDirection = Direction.BALANCE,
                    magnitude = abs(diffs.brightnessDiffPercent),
                    priority = if (abs(diffs.brightnessDiffPercent) > LARGE_DIFF_THRESHOLD) Priority.HIGH else Priority.MEDIUM
                )
            )
        }

        if (abs(diffs.contrastDiffPercent) > SIGNIFICANT_DIFF_THRESHOLD) {
            recs.add(
                Recommendation(
                    metric = "Contrast",
                    description = describeContrastDiff(diffs.contrastDiffPercent),
                    suggestedDirection = Direction.BALANCE,
                    magnitude = abs(diffs.contrastDiffPercent),
                    priority = if (abs(diffs.contrastDiffPercent) > LARGE_DIFF_THRESHOLD) Priority.HIGH else Priority.MEDIUM
                )
            )
        }

        if (abs(diffs.saturationDiffPercent) > SIGNIFICANT_DIFF_THRESHOLD) {
            recs.add(
                Recommendation(
                    metric = "Saturation",
                    description = describeSaturationDiff(diffs.saturationDiffPercent, stock, camera),
                    suggestedDirection = Direction.BALANCE,
                    magnitude = abs(diffs.saturationDiffPercent),
                    priority = if (abs(diffs.saturationDiffPercent) > LARGE_DIFF_THRESHOLD) Priority.HIGH else Priority.MEDIUM
                )
            )
        }

        if (abs(diffs.sharpnessDiffPercent) > SIGNIFICANT_DIFF_THRESHOLD) {
            recs.add(
                Recommendation(
                    metric = "Sharpness",
                    description = describeSharpnessDiff(diffs.sharpnessDiffPercent),
                    suggestedDirection = Direction.BALANCE,
                    magnitude = abs(diffs.sharpnessDiffPercent),
                    priority = Priority.LOW
                )
            )
        }

        if (abs(diffs.noiseDiffPercent) > SIGNIFICANT_DIFF_THRESHOLD) {
            recs.add(
                Recommendation(
                    metric = "Noise",
                    description = describeNoiseDiff(diffs.noiseDiffPercent),
                    suggestedDirection = if (diffs.noiseDiffPercent > 0) Direction.DECREASE else Direction.INCREASE,
                    magnitude = abs(diffs.noiseDiffPercent),
                    priority = Priority.MEDIUM
                )
            )
        }

        return recs
    }

    private fun describeBrightnessDiff(diffPercent: Float, stock: ImageMetrics, camera: ImageMetrics): String {
        val cameraPct = (camera.perceivedBrightness * 100f).toInt()
        val stockPct = (stock.perceivedBrightness * 100f).toInt()
        return if (diffPercent > 0) {
            "Stock camera is ${abs(diffPercent).toInt()}% brighter than ours " +
                "(stock: ${stockPct}%, ours: ${cameraPct}%). " +
                "Our camera output appears dull by comparison. " +
                "Natural balance: boost perceived brightness toward midpoint."
        } else {
            "Our camera is ${abs(diffPercent).toInt()}% brighter than stock " +
                "(ours: ${cameraPct}%, stock: ${stockPct}%). " +
                "Stock may be over-processed. " +
                "Natural balance: retain our brightness, minor vibrance lift."
        }
    }

    private fun describeContrastDiff(diffPercent: Float): String {
        return if (diffPercent > 0) {
            "Stock is ${abs(diffPercent).toInt()}% higher contrast. " +
                "May appear over-processed. " +
                "Balance: moderate local contrast improvement without harsh global shift."
        } else {
            "Our camera is ${abs(diffPercent).toInt()}% higher contrast. " +
                "May appear flat compared to stock. " +
                "Balance: slight contrast reduction toward natural midpoint."
        }
    }

    private fun describeSaturationDiff(diffPercent: Float, stock: ImageMetrics, camera: ImageMetrics): String {
        val cameraSat = (camera.saturation.meanSaturation * 100f).toInt()
        val stockSat = (stock.saturation.meanSaturation * 100f).toInt()
        return if (diffPercent > 0) {
            "Stock is ${abs(diffPercent).toInt()}% more saturated (stock: ${stockSat}%, ours: ${cameraSat}%). " +
                "Stock colors appear more vibrant but may look artificial. " +
                "Balance: slight vibrance lift, preserve natural color accuracy."
        } else {
            "Our camera is ${abs(diffPercent).toInt()}% more saturated (ours: ${cameraSat}%, stock: ${stockSat}%). " +
                "Unusual — our camera may be over-saturating. " +
                "Balance: reduce saturation toward natural midpoint."
        }
    }

    private fun describeSharpnessDiff(diffPercent: Float): String {
        return if (diffPercent > 0) {
            "Stock is ${abs(diffPercent).toInt()}% sharper. " +
                "May indicate over-sharpening artifacts. " +
                "Balance: preserve natural edge definition, avoid artificial sharpening."
        } else {
            "Our camera is ${abs(diffPercent).toInt()}% sharper. " +
                "Good edge definition. " +
                "Balance: maintain current sharpness level."
        }
    }

    private fun describeNoiseDiff(diffPercent: Float): String {
        return if (diffPercent > 0) {
            "Stock has ${abs(diffPercent).toInt()}% more noise. " +
                "May indicate less aggressive noise reduction. " +
                "Balance: preserve detail while controlling noise floor."
        } else {
            "Our camera has ${abs(diffPercent).toInt()}% more noise. " +
                "May indicate insufficient noise reduction. " +
                "Balance: apply moderate denoising without losing detail."
        }
    }

    fun percentDiff(a: Float, b: Float): Float {
        val avg = (a + b) / 2f
        if (avg == 0f) return 0f
        return ((a - b) / avg) * 100f
    }
}
