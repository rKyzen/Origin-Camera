/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jetpackcamera.ui.components.capture

import android.view.OrientationEventListener.ORIENTATION_UNKNOWN
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

val BracketColor = Color(0x80FFFFFF)
private val ToolbarBg = Color(0xCC1A1A1A)
private val PillBg = Color(0xE6FFFFFF)
private val ZoomPillBg = Color(0x99333333)
private val FrostedPill = Color(0xCC2A2A2A)
private val FrostedBorder = Color.White.copy(alpha = 0.15f)

@Composable
fun PreviewLayout(
    modifier: Modifier = Modifier,
    viewfinder: @Composable (Modifier) -> Unit,
    captureButton: @Composable (Modifier) -> Unit = {},
    imageWell: @Composable (Modifier) -> Unit = {},
    flipCameraButton: @Composable (Modifier) -> Unit = {},
    zoomLevelDisplay: @Composable (Modifier) -> Unit = {},
    elapsedTimeDisplay: @Composable (Modifier) -> Unit = {},
    quickSettingsButton: @Composable (Modifier) -> Unit = {},
    indicatorRow: @Composable (Modifier) -> Unit = {},
    captureModeToggle: @Composable (Modifier) -> Unit = {},
    quickSettingsOverlay: @Composable (Modifier) -> Unit = {},
    debugOverlay: @Composable (Modifier) -> Unit = {},
    debugVisibilityWrapper: (@Composable (@Composable () -> Unit) -> Unit) = { it() },
    screenFlashOverlay: @Composable (Modifier) -> Unit = {},
    snackBar: @Composable (Modifier, snackbarHostState: SnackbarHostState) -> Unit = { _, _ -> },
    aspectRatioLabel: String = "3:4",
    lensLabel: String = "23 MM",
    currentEvIndex: Int = 0,
    onEvChange: (Int) -> Unit = {},
    onCapture: () -> Unit = {},
    isRecording: Boolean = false,
    isVideoMode: Boolean = false,
    onToggleCaptureMode: () -> Unit = {},
    isUltraWide: Boolean = false,
    onFlipLens: () -> Unit = {},
    onTrackpadFocusChange: (relX: Float, relY: Float) -> Unit = { _, _ -> },
    currentZoomRatio: Float = 1f,
    minZoomRatio: Float = 1f,
    maxZoomRatio: Float = 1f,
    onZoomChange: (Float) -> Unit = {},
    currentShutterSpeed: Int = 60,
    onShutterSpeedChange: (Int) -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onFiltersClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Viewfinder: 80% of screen ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .fillMaxHeight(0.80f)
                    .clip(RoundedCornerShape(40.dp))
            ) {
                viewfinder(Modifier.fillMaxSize())

                Text(
                    text = "$aspectRatioLabel  $lensLabel",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 50.dp)
                )

                var deviceOrientation by remember { mutableIntStateOf(ORIENTATION_UNKNOWN) }
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    rawOrientationDegreesFlow(context).collect { deviceOrientation = it }
                }

                LevelIndicator(
                    deviceOrientation = deviceOrientation,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(width = 160.dp, height = 36.dp)
                )

                elapsedTimeDisplay(Modifier.align(Alignment.TopCenter).padding(top = 34.dp))

                HorizontalZoomSlider(
                    currentZoomRatio = currentZoomRatio,
                    minZoomRatio = minZoomRatio,
                    maxZoomRatio = maxZoomRatio,
                    onZoomChange = onZoomChange,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                        .width(240.dp)
                        .height(36.dp)
                )

                snackBar(Modifier, snackbarHostState)
                screenFlashOverlay(Modifier)
            }

            // ── Shutter + flip: half in viewfinder, half out ────────
            Box(
                modifier = Modifier
                    .offset(y = (-39).dp)
                    .size(width = 220.dp, height = 78.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 220.dp, height = 78.dp)
                        .clip(RoundedCornerShape(39.dp))
                        .background(FrostedPill)
                        .border(0.5.dp, FrostedBorder, RoundedCornerShape(39.dp))
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    flipCameraButton(Modifier)
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 62.dp)
                            .shadow(8.dp, RoundedCornerShape(31.dp))
                            .clip(RoundedCornerShape(31.dp))
                            .background(PillBg)
                            .pointerInput(Unit) { detectTapGestures { onCapture() } }
                    )
                }
            }

            // ── Controls: remaining space ─────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                BottomToolbar(
                    modifier = Modifier.padding(top = 8.dp),
                    onGalleryClick = onGalleryClick,
                    onFiltersClick = onFiltersClick,
                    onSettingsClick = onSettingsClick
                )
            }
        }

        // ── Corner brackets: full-screen overlay ──────────────────────
        CornerBrackets(modifier = Modifier.fillMaxSize())

        quickSettingsOverlay(Modifier)
        debugOverlay(Modifier)
    }
}

@Composable
fun CornerBrackets(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val len = 24.dp.toPx(); val s = 2.5.dp.toPx()
        val margin = 28.dp.toPx()
        val topY = 18.dp.toPx()
        val bottomY = h * 0.72f
        val style = Stroke(s, cap = StrokeCap.Round)

        drawPath(Path().apply { moveTo(margin, topY + len); lineTo(margin, topY); lineTo(margin + len, topY) },
            BracketColor, style = style)
        drawPath(Path().apply { moveTo(w - margin - len, topY); lineTo(w - margin, topY); lineTo(w - margin, topY + len) },
            BracketColor, style = style)
        drawPath(Path().apply { moveTo(margin, bottomY - len); lineTo(margin, bottomY); lineTo(margin + len, bottomY) },
            BracketColor, style = style)
        drawPath(Path().apply { moveTo(w - margin - len, bottomY); lineTo(w - margin, bottomY); lineTo(w - margin, bottomY - len) },
            BracketColor, style = style)
    }
}

@Composable
fun LevelIndicator(
    deviceOrientation: Int,
    modifier: Modifier = Modifier
) {
    val isUnknown = deviceOrientation == ORIENTATION_UNKNOWN
    val currentRef = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(deviceOrientation) {
        if (deviceOrientation != ORIENTATION_UNKNOWN) {
            val d = deviceOrientation.toFloat()
            var diff = d - currentRef.floatValue
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f
            if (diff > 60f) {
                currentRef.floatValue = ((currentRef.floatValue + 90f) % 360f + 360f) % 360f
            } else if (diff < -60f) {
                currentRef.floatValue = ((currentRef.floatValue - 90f) % 360f + 360f) % 360f
            }
        }
    }

    val tilt = if (isUnknown) 0f else {
        val d = deviceOrientation.toFloat()
        var diff = d - currentRef.floatValue
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        diff
    }

    val animatedTilt by animateFloatAsState(
        targetValue = tilt,
        animationSpec = tween(durationMillis = 200)
    )

    val isLevel = abs(animatedTilt) < 2f
    val lineColor = if (isLevel) Color(0xFFFFD700) else Color.White.copy(alpha = 0.55f)

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val s = 1.5.dp.toPx(); val cx = w / 2f; val cy = h / 2f
        rotate(animatedTilt, pivot = Offset(cx, cy)) {
            drawRoundRect(lineColor, Offset(w * 0.3f, h * 0.1f), Size(w * 0.4f, h * 0.8f), CornerRadius(12.dp.toPx()), style = Stroke(s))
            drawLine(lineColor, Offset(0f, cy), Offset(w * 0.28f, cy), s)
            drawLine(lineColor, Offset(w * 0.72f, cy), Offset(w, cy), s)

            if (!isLevel && !isUnknown) {
                val as_ = 5.dp.toPx()
                val sign = if (animatedTilt > 0) 1f else -1f
                Path().apply {
                    moveTo(w * 0.28f, cy)
                    lineTo(w * 0.28f - as_ * 0.5f, cy + sign * as_)
                    lineTo(w * 0.28f + as_ * 0.5f, cy + sign * as_)
                    close()
                }.let { drawPath(it, lineColor) }
                Path().apply {
                    moveTo(w * 0.72f, cy)
                    lineTo(w * 0.72f - as_ * 0.5f, cy - sign * as_)
                    lineTo(w * 0.72f + as_ * 0.5f, cy - sign * as_)
                    close()
                }.let { drawPath(it, lineColor) }
            }
        }
    }
}

@Composable
private fun HorizontalZoomSlider(
    currentZoomRatio: Float, minZoomRatio: Float, maxZoomRatio: Float,
    onZoomChange: (Float) -> Unit, modifier: Modifier = Modifier
) {
    val range = maxZoomRatio - minZoomRatio
    val normalizedPos = if (range > 0f) ((currentZoomRatio - minZoomRatio) / range).coerceIn(0f, 1f) else 0.5f
    val zoomLabel = if (currentZoomRatio <= 1.05f) "1x"
    else if (currentZoomRatio < 10f) "${"%.1f".format(currentZoomRatio)}x"
    else "${currentZoomRatio.roundToInt()}x"

    val currentOnZoomChange by rememberUpdatedState(onZoomChange)

    Box(modifier = modifier
            .pointerInput(minZoomRatio, maxZoomRatio) {
                detectDragGestures(onDrag = { change, _ ->
                    val norm = (change.position.x / size.width).coerceIn(0f, 1f)
                    currentOnZoomChange(minZoomRatio + norm * range)
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cy = size.height / 2f; val tc = 25; val sp = size.width / tc
            for (i in 0 until tc) {
                val x = i * sp + sp / 2f; val major = i % 5 == 0
                val th = if (major) 14.dp.toPx() else 8.dp.toPx()
                val tw = if (major) 2.dp.toPx() else 1.dp.toPx()
                val color = if (major) Color.White.copy(0.7f) else Color.White.copy(0.35f)
                drawRoundRect(color, Offset(x - tw / 2f, cy - th / 2f), Size(tw, th), CornerRadius(tw / 2f))
            }
        }
        Box(
            modifier = Modifier.offset(x = ((normalizedPos - 0.5f) * 196).dp)
                .size(width = 44.dp, height = 26.dp).clip(RoundedCornerShape(13.dp)).background(ZoomPillBg),
            contentAlignment = Alignment.Center
        ) { Text(zoomLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun BottomToolbar(
    modifier: Modifier = Modifier,
    onGalleryClick: () -> Unit = {},
    onFiltersClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Row(modifier = modifier
            .clip(RoundedCornerShape(20.dp)).background(ToolbarBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gallery (red cube)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF8B0000))
                .clickable(onClick = onGalleryClick)
        )
        // Filters (circles)
        Canvas(
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onFiltersClick)
        ) {
            val r = 8.dp.toPx(); val o = 4.dp.toPx()
            drawCircle(Color.White.copy(0.7f), r, Offset(size.width / 2f - o, size.height / 2f))
            drawCircle(Color.White.copy(0.5f), r, Offset(size.width / 2f + o, size.height / 2f - o))
            drawCircle(Color.White.copy(0.5f), r, Offset(size.width / 2f + o, size.height / 2f + o))
        }
        // Sliders (ignore)
        Canvas(Modifier.size(28.dp)) {
            val s = 1.5.dp.toPx(); val c = Color.White.copy(0.7f); val ll = 18.dp.toPx(); val sx = (size.width - ll) / 2f
            for (i in 0..2) { val y = size.height * (0.25f + i * 0.25f); drawLine(c, Offset(sx, y), Offset(sx + ll, y), s); drawCircle(c, 2.dp.toPx(), Offset(sx + ll + 4.dp.toPx(), y)) }
        }
        // Settings (circle)
        Canvas(
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onSettingsClick)
        ) {
            val c = Color.White.copy(0.7f); val ct = Offset(size.width / 2f, size.height / 2f); val s = 1.5.dp.toPx()
            drawCircle(c, 9.dp.toPx(), ct, style = Stroke(s)); drawCircle(c, 5.dp.toPx(), ct, style = Stroke(s))
        }
    }
}

@Preview(widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CaptureLayoutPreview() {
    PreviewLayout(
        viewfinder = { m -> Box(m.fillMaxSize().background(Color(0xFF2D4A2D))) },
        aspectRatioLabel = "9:12", lensLabel = "23mm",
        currentEvIndex = 0, onEvChange = {}, isRecording = false, isVideoMode = false
    )
}
