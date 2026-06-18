package com.google.jetpackcamera.core.camera.mfs

import android.graphics.Bitmap

data class FrameData(
    val bitmap: Bitmap,
    val index: Int,
    val timestampMs: Long
) {
    val width: Int get() = bitmap.width
    val height: Int get() = bitmap.height
}
