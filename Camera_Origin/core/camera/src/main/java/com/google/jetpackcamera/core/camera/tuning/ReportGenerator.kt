package com.google.jetpackcamera.core.camera.tuning

object ReportGenerator {

    fun generate(sceneReport: SceneReport): String {
        val sb = StringBuilder()
        sb.appendLine("═══════════════════════════════════════════════════════════")
        sb.appendLine("  SCENE: ${sceneReport.sceneName}")
        sb.appendLine("═══════════════════════════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("── METRIC COMPARISON ──────────────────────────────────────")
        sb.appendLine()
        appendMetricTable(sb, sceneReport)
        sb.appendLine()
        sb.appendLine("── RECOMMENDATIONS ────────────────────────────────────────")
        sb.appendLine()
        for (rec in sceneReport.recommendations) {
            sb.appendLine("  [${rec.priority}] ${rec.metric}")
            sb.appendLine("    ${rec.description}")
            sb.appendLine()
        }
        sb.appendLine("── BALANCED TARGET PROFILE ────────────────────────────────")
        sb.appendLine()
        appendBalancedProfile(sb, sceneReport.balancedProfile, sceneReport.stockMetrics, sceneReport.cameraMetrics)
        sb.appendLine()
        sb.appendLine("═══════════════════════════════════════════════════════════")
        return sb.toString()
    }

    fun generateFullReport(report: DatasetReport): String {
        val sb = StringBuilder()
        sb.appendLine("╔═══════════════════════════════════════════════════════════╗")
        sb.appendLine("║         CAMERA TUNING ANALYSIS — FULL REPORT            ║")
        sb.appendLine("╚═══════════════════════════════════════════════════════════╝")
        sb.appendLine()
        sb.appendLine("Scenes analyzed: ${report.sceneReports.size}")
        sb.appendLine()
        for (sceneReport in report.sceneReports) {
            sb.appendLine(generate(sceneReport))
            sb.appendLine()
        }
        sb.appendLine("╔═══════════════════════════════════════════════════════════╗")
        sb.appendLine("║                  OVERALL SUMMARY                        ║")
        sb.appendLine("╚═══════════════════════════════════════════════════════════╝")
        sb.appendLine()
        sb.appendLine("── AVERAGE DIFFERENCES ────────────────────────────────────")
        sb.appendLine()
        appendDifferenceTable(sb, report.averageDifferences)
        sb.appendLine()
        sb.appendLine("── GLOBAL RECOMMENDATIONS ─────────────────────────────────")
        sb.appendLine()
        for (rec in report.overallRecommendations) {
            sb.appendLine("  [${rec.priority}] ${rec.metric}")
            sb.appendLine("    ${rec.description}")
            sb.appendLine()
        }
        sb.appendLine("── SUMMARY ────────────────────────────────────────────────")
        sb.appendLine()
        sb.appendLine("  ${report.summary}")
        sb.appendLine()
        sb.appendLine("═══════════════════════════════════════════════════════════")
        return sb.toString()
    }

    private fun appendMetricTable(sb: StringBuilder, report: SceneReport) {
        fun diffStr(v: Float) = if (v >= 0) "+%.1f%%".format(v) else "%.1f%%".format(v)

        sb.appendLine("  %-14s  %10s  %10s  %10s".format("Metric", "Stock", "Ours", "Diff%"))
        sb.appendLine("  %-14s  %10s  %10s  %10s".format("─────────────", "─────────", "─────────", "─────────"))
        sb.appendLine(
            "  %-14s  %10s  %10s  %10s".format(
                "Luminance",
                "%.1f".format(report.stockMetrics.luminance.mean),
                "%.1f".format(report.cameraMetrics.luminance.mean),
                diffStr(report.differences.luminanceDiffPercent)
            )
        )
        sb.appendLine(
            "  %-14s  %10s  %10s  %10s".format(
                "Contrast",
                "%.3f".format(report.stockMetrics.contrast.rmsContrast),
                "%.3f".format(report.cameraMetrics.contrast.rmsContrast),
                diffStr(report.differences.contrastDiffPercent)
            )
        )
        sb.appendLine(
            "  %-14s  %10s  %10s  %10s".format(
                "Saturation",
                "%.3f".format(report.stockMetrics.saturation.meanSaturation),
                "%.3f".format(report.cameraMetrics.saturation.meanSaturation),
                diffStr(report.differences.saturationDiffPercent)
            )
        )
        sb.appendLine(
            "  %-14s  %10s  %10s  %10s".format(
                "Sharpness",
                "%.3f".format(report.stockMetrics.sharpness.laplacianVariance),
                "%.3f".format(report.cameraMetrics.sharpness.laplacianVariance),
                diffStr(report.differences.sharpnessDiffPercent)
            )
        )
        sb.appendLine(
            "  %-14s  %10s  %10s  %10s".format(
                "Noise",
                "%.3f".format(report.stockMetrics.noise.estimatedNoise),
                "%.3f".format(report.cameraMetrics.noise.estimatedNoise),
                diffStr(report.differences.noiseDiffPercent)
            )
        )
        sb.appendLine(
            "  %-14s  %10s  %10s  %10s".format(
                "Brightness",
                "%.1f%%".format(report.stockMetrics.perceivedBrightness * 100f),
                "%.1f%%".format(report.cameraMetrics.perceivedBrightness * 100f),
                diffStr(report.differences.brightnessDiffPercent)
            )
        )
    }

    private fun appendDifferenceTable(sb: StringBuilder, diffs: MetricDifferences) {
        fun diffStr(v: Float) = if (v >= 0) "+%.1f%%".format(v) else "%.1f%%".format(v)

        sb.appendLine("  %-14s  %10s".format("Metric", "Avg Diff%"))
        sb.appendLine("  %-14s  %10s".format("─────────────", "─────────"))
        sb.appendLine("  %-14s  %10s".format("Luminance", diffStr(diffs.luminanceDiffPercent)))
        sb.appendLine("  %-14s  %10s".format("Contrast", diffStr(diffs.contrastDiffPercent)))
        sb.appendLine("  %-14s  %10s".format("Saturation", diffStr(diffs.saturationDiffPercent)))
        sb.appendLine("  %-14s  %10s".format("Sharpness", diffStr(diffs.sharpnessDiffPercent)))
        sb.appendLine("  %-14s  %10s".format("Noise", diffStr(diffs.noiseDiffPercent)))
        sb.appendLine("  %-14s  %10s".format("Brightness", diffStr(diffs.brightnessDiffPercent)))
    }

    private fun appendBalancedProfile(sb: StringBuilder, profile: BalancedProfile, stock: ImageMetrics, camera: ImageMetrics) {
        sb.appendLine("  Target values (balanced midpoint between stock and camera):")
        sb.appendLine()
        sb.appendLine("  %-14s  %10s  (stock: %.3f, ours: %.3f)".format(
            "Luminance",
            "%.1f".format(profile.targetLuminance),
            stock.luminance.mean,
            camera.luminance.mean
        ))
        sb.appendLine("  %-14s  %10s  (stock: %.3f, ours: %.3f)".format(
            "Contrast",
            "%.3f".format(profile.targetContrast),
            stock.contrast.rmsContrast,
            camera.contrast.rmsContrast
        ))
        sb.appendLine("  %-14s  %10s  (stock: %.3f, ours: %.3f)".format(
            "Saturation",
            "%.3f".format(profile.targetSaturation),
            stock.saturation.meanSaturation,
            camera.saturation.meanSaturation
        ))
        sb.appendLine("  %-14s  %10s  (stock: %.3f, ours: %.3f)".format(
            "Sharpness",
            "%.3f".format(profile.targetSharpness),
            stock.sharpness.laplacianVariance,
            camera.sharpness.laplacianVariance
        ))
        sb.appendLine("  %-14s  %10s  (stock: %.3f, ours: %.3f)".format(
            "Noise",
            "%.3f".format(profile.targetNoise),
            stock.noise.estimatedNoise,
            camera.noise.estimatedNoise
        ))
        sb.appendLine("  %-14s  %10s  (stock: %.1f%%, ours: %.1f%%)".format(
            "Brightness",
            "%.1f%%".format(profile.targetBrightness * 100f),
            stock.perceivedBrightness * 100f,
            camera.perceivedBrightness * 100f
        ))
    }
}
