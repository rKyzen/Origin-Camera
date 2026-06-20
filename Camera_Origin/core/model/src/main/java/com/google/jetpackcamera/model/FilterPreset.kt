package com.google.jetpackcamera.model

import android.graphics.ColorMatrix

enum class FilterPreset(
    val label: String,
    val colorMatrix: FloatArray
) {
    DEFAULT(
        label = "Default",
        colorMatrix = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    VIVID(
        label = "Vivid",
        colorMatrix = floatArrayOf(
            1.15f, 0.05f, 0.0f, 0f, 0f,
            0.0f, 1.15f, 0.0f, 0f, 0f,
            0.0f, 0.0f, 1.1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    BW(
        label = "B&W",
        colorMatrix = floatArrayOf(
            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    );

    fun applyToBitmap(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        val result = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(result)
        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(ColorMatrix(colorMatrix))
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
}
