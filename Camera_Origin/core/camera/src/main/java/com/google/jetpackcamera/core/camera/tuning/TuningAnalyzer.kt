package com.google.jetpackcamera.core.camera.tuning

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File

class TuningAnalyzer {

    fun analyzePair(
        stockBitmap: Bitmap,
        cameraBitmap: Bitmap,
        sceneName: String
    ): SceneReport {
        Log.d(TAG, "Analyzing scene: $sceneName")
        val stockMetrics = analyzeImage(stockBitmap)
        val cameraMetrics = analyzeImage(cameraBitmap)
        return SceneComparison.compare(sceneName, stockMetrics, cameraMetrics)
    }

    fun analyzeImage(bitmap: Bitmap): ImageMetrics {
        val luminance = LuminanceAnalyzer.analyze(bitmap)
        val contrast = ContrastAnalyzer.analyze(bitmap)
        val saturation = SaturationAnalyzer.analyze(bitmap)
        val sharpness = SharpnessAnalyzer.analyze(bitmap)
        val noise = NoiseEstimator.estimate(bitmap)
        val brightness = BrightnessEstimator.estimate(bitmap)
        return ImageMetrics(
            luminance = luminance,
            contrast = contrast,
            saturation = saturation,
            sharpness = sharpness,
            noise = noise,
            perceivedBrightness = brightness
        )
    }

    fun analyzeDataset(stockDir: File, cameraDir: File): DatasetReport {
        val sceneReports = mutableListOf<SceneReport>()
        val stockFiles = stockDir.listFiles { f -> f.isFile && f.extension.equals("jpg", ignoreCase = true) }
            ?.sortedBy { it.name } ?: emptyList()

        for (stockFile in stockFiles) {
            val sceneName = stockFile.nameWithoutExtension
            val cameraFile = File(cameraDir, stockFile.name)
            if (!cameraFile.exists()) {
                Log.w(TAG, "No matching camera file for $sceneName, skipping")
                continue
            }
            val stockBitmap = loadBitmap(stockFile) ?: continue
            val cameraBitmap = loadBitmap(cameraFile) ?: continue
            try {
                val report = analyzePair(stockBitmap, cameraBitmap, sceneName)
                sceneReports.add(report)
                Log.d(TAG, "Completed analysis for scene: $sceneName")
            } finally {
                stockBitmap.recycle()
                cameraBitmap.recycle()
            }
        }

        val avgDiffs = computeAverageDifferences(sceneReports)
        val overallRecs = generateOverallRecommendations(sceneReports, avgDiffs)
        val summary = generateSummary(sceneReports, avgDiffs)

        return DatasetReport(
            sceneReports = sceneReports,
            overallRecommendations = overallRecs,
            averageDifferences = avgDiffs,
            summary = summary
        )
    }

    private fun loadBitmap(file: File): Bitmap? {
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap: ${file.absolutePath}", e)
            null
        }
    }

    private fun computeAverageDifferences(reports: List<SceneReport>): MetricDifferences {
        if (reports.isEmpty()) {
            return MetricDifferences(0f, 0f, 0f, 0f, 0f, 0f)
        }
        var lumSum = 0f
        var conSum = 0f
        var satSum = 0f
        var sharpSum = 0f
        var noiseSum = 0f
        var brightSum = 0f
        for (r in reports) {
            lumSum += r.differences.luminanceDiffPercent
            conSum += r.differences.contrastDiffPercent
            satSum += r.differences.saturationDiffPercent
            sharpSum += r.differences.sharpnessDiffPercent
            noiseSum += r.differences.noiseDiffPercent
            brightSum += r.differences.brightnessDiffPercent
        }
        val n = reports.size.toFloat()
        return MetricDifferences(
            luminanceDiffPercent = lumSum / n,
            contrastDiffPercent = conSum / n,
            saturationDiffPercent = satSum / n,
            sharpnessDiffPercent = sharpSum / n,
            noiseDiffPercent = noiseSum / n,
            brightnessDiffPercent = brightSum / n
        )
    }

    private fun generateOverallRecommendations(
        reports: List<SceneReport>,
        avgDiffs: MetricDifferences
    ): List<Recommendation> {
        val recs = mutableListOf<Recommendation>()
        if (kotlin.math.abs(avgDiffs.brightnessDiffPercent) > 5f) {
            recs.add(
                Recommendation(
                    metric = "Overall Brightness",
                    description = if (avgDiffs.brightnessDiffPercent > 0) {
                        "Across all scenes, stock camera is ${kotlin.math.abs(avgDiffs.brightnessDiffPercent).toInt()}% " +
                            "brighter on average. Our images appear duller. " +
                            "Recommendation: raise minimum perceptual brightness floor without " +
                            "over-brightening highlights. Target the balanced midpoint."
                    } else {
                        "Across all scenes, our camera is ${kotlin.math.abs(avgDiffs.brightnessDiffPercent).toInt()}% " +
                            "brighter on average. Stock may be under-exposed. " +
                            "Recommendation: maintain our brightness advantage."
                    },
                    suggestedDirection = Direction.BALANCE,
                    magnitude = kotlin.math.abs(avgDiffs.brightnessDiffPercent),
                    priority = Priority.HIGH
                )
            )
        }
        if (kotlin.math.abs(avgDiffs.saturationDiffPercent) > 5f) {
            recs.add(
                Recommendation(
                    metric = "Overall Saturation",
                    description = if (avgDiffs.saturationDiffPercent > 0) {
                        "Stock is ${kotlin.math.abs(avgDiffs.saturationDiffPercent).toInt()}% more saturated. " +
                            "Colors look more vivid but potentially artificial. " +
                            "Recommendation: moderate vibrance boost targeting midtones, " +
                            "preserve skin tone accuracy."
                    } else {
                        "Our camera is ${kotlin.math.abs(avgDiffs.saturationDiffPercent).toInt()}% more saturated. " +
                            "Unusual — verify color pipeline. " +
                            "Recommendation: reduce saturation toward natural midpoint."
                    },
                    suggestedDirection = Direction.BALANCE,
                    magnitude = kotlin.math.abs(avgDiffs.saturationDiffPercent),
                    priority = Priority.MEDIUM
                )
            )
        }
        if (kotlin.math.abs(avgDiffs.contrastDiffPercent) > 5f) {
            recs.add(
                Recommendation(
                    metric = "Overall Contrast",
                    description = if (avgDiffs.contrastDiffPercent > 0) {
                        "Stock shows ${kotlin.math.abs(avgDiffs.contrastDiffPercent).toInt()}% higher contrast. " +
                            "May indicate aggressive tone mapping. " +
                            "Recommendation: improve local contrast selectively (edges, textures) " +
                            "without harsh global S-curve."
                    } else {
                        "Our camera shows ${kotlin.math.abs(avgDiffs.contrastDiffPercent).toInt()}% higher contrast. " +
                            "Verify not over-processing. " +
                            "Recommendation: slight contrast moderation toward natural midpoint."
                    },
                    suggestedDirection = Direction.BALANCE,
                    magnitude = kotlin.math.abs(avgDiffs.contrastDiffPercent),
                    priority = Priority.MEDIUM
                )
            )
        }
        if (reports.isNotEmpty()) {
            recs.add(
                Recommendation(
                    metric = "Calibration Guidance",
                    description = "Balanced profile computed from ${reports.size} scenes. " +
                        "Target values represent a middle ground that preserves natural lighting " +
                        "while improving perceptual appeal. Apply targets as soft guidelines, " +
                        "not hard constraints. Re-analyze after any pipeline changes.",
                    suggestedDirection = Direction.BALANCE,
                    magnitude = 0f,
                    priority = Priority.LOW
                )
            )
        }
        return recs
    }

    private fun generateSummary(reports: List<SceneReport>, avgDiffs: MetricDifferences): String {
        if (reports.isEmpty()) return "No scenes analyzed."
        val brightnessDir = if (avgDiffs.brightnessDiffPercent > 0) "duller" else "brighter"
        val saturationDir = if (avgDiffs.saturationDiffPercent > 0) "less saturated" else "more saturated"
        val contrastDir = if (avgDiffs.contrastDiffPercent > 0) "lower" else "higher"
        return "Analyzed ${reports.size} scenes. " +
            "Our camera output is ${kotlin.math.abs(avgDiffs.brightnessDiffPercent).toInt()}% $brightnessDir, " +
            "${kotlin.math.abs(avgDiffs.saturationDiffPercent).toInt()}% $saturationDir, " +
            "and ${kotlin.math.abs(avgDiffs.contrastDiffPercent).toInt()}% $contrastDir contrast " +
            "compared to stock camera. " +
            "The balanced target profile aims for a middle ground: " +
            "slightly brighter than current output to avoid dullness, " +
            "with moderate vibrance improvement, while preserving natural lighting accuracy."
    }

    companion object {
        private const val TAG = "TuningAnalyzer"
    }
}
