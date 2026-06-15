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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

// ── Design tokens ─────────────────────────────────────────────────────────────

private val OriginOrange = Color(0xFFFF8C00)

// Frosted glass panels — semi-transparent so camera bleeds through
private val PanelBg     = Color(0xB32C2C2C)   // 70% opaque dark gray
private val PanelBorder = Color.White.copy(alpha = 0.18f)
private val LightGray   = Color(0xFFCCCCCC)
private val DimGray     = Color(0xFF888888)
private val TrackpadBg  = Color(0xE6F0F0F0)   // 90% opaque off-white

private val SHUTTER_SPEEDS = listOf(4000, 2000, 1000, 500, 250, 125, 60, 30, 15, 8, 4, 2, 1)
private val CELL_SHAPE    = RoundedCornerShape(14.dp)
private val GAP           = 4.dp
private val PANEL_PADDING = 4.dp
private val CELL_BORDER  = 0.5.dp

// ── Public layout API ─────────────────────────────────────────────────────────

/**
 * Origin Camera landscape layout.
 *
 * The viewfinder fills ~60 % of the width; the right panel occupies the rest
 * and is divided into a 3-column x 3-row control grid with frosted glass cells.
 * The shutter-speed strip spans the full height of column 1.
 */
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
    // Origin-specific controls
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
    // Zoom slider state
    currentZoomRatio: Float = 1f,
    minZoomRatio: Float = 1f,
    maxZoomRatio: Float = 1f,
    onZoomChange: (Float) -> Unit = {},
    // Shutter speed
    currentShutterSpeed: Int = 60,
    onShutterSpeedChange: (Int) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // ── LEFT: viewfinder ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(0.60f)
                    .fillMaxHeight()
            ) {
                viewfinder(Modifier.fillMaxSize())

                // Aspect ratio label – top-left, plain text overlay
                Text(
                    text = aspectRatioLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )

                // Focal-length label – bottom-left, plain text overlay
                Text(
                    text = lensLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )

                // Elapsed-time badge (visible during recording)
                elapsedTimeDisplay(Modifier.align(Alignment.TopCenter).padding(top = 10.dp))

                // Snackbar & flash stay inside the viewfinder column
                snackBar(Modifier, snackbarHostState)
                screenFlashOverlay(Modifier)
            }

            // ── RIGHT: control panel ─────────────────────────────────────────
            debugVisibilityWrapper {
                OriginControlPanel(
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight(),
                    currentEvIndex = currentEvIndex,
                    onEvChange = onEvChange,
                    onCapture = onCapture,
                    isRecording = isRecording,
                    isVideoMode = isVideoMode,
                    onToggleCaptureMode = onToggleCaptureMode,
                    isUltraWide = isUltraWide,
                    onFlipLens = onFlipLens,
                    onTrackpadFocusChange = onTrackpadFocusChange,
                    quickSettingsButton = quickSettingsButton,
                    currentZoomRatio = currentZoomRatio,
                    minZoomRatio = minZoomRatio,
                    maxZoomRatio = maxZoomRatio,
                    onZoomChange = onZoomChange,
                    currentShutterSpeed = currentShutterSpeed,
                    onShutterSpeedChange = onShutterSpeedChange
                )
            }
        }

        // Quick-settings sheet can cover everything
        quickSettingsOverlay(Modifier)
        // Debug overlay on top
        debugOverlay(Modifier)
    }
}

// ── Frosted glass cell wrapper ────────────────────────────────────────────────

@Composable
private fun GlassCell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CELL_SHAPE)
            .background(PanelBg)
            .border(CELL_BORDER, PanelBorder, CELL_SHAPE),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ── Control panel ─────────────────────────────────────────────────────────────

@Composable
private fun OriginControlPanel(
    modifier: Modifier = Modifier,
    currentEvIndex: Int,
    onEvChange: (Int) -> Unit,
    onCapture: () -> Unit,
    isRecording: Boolean,
    isVideoMode: Boolean,
    onToggleCaptureMode: () -> Unit,
    isUltraWide: Boolean,
    onFlipLens: () -> Unit,
    onTrackpadFocusChange: (Float, Float) -> Unit,
    quickSettingsButton: @Composable (Modifier) -> Unit,
    currentZoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    currentShutterSpeed: Int,
    onShutterSpeedChange: (Int) -> Unit
) {
    Row(
        modifier = modifier
            .padding(PANEL_PADDING),
        horizontalArrangement = Arrangement.spacedBy(GAP)
    ) {

        // ── Column 1: shutter-speed strip (full height) ─────────────────────
        ShutterSpeedStrip(
            currentSpeed = currentShutterSpeed,
            onSpeedChange = onShutterSpeedChange,
            modifier = Modifier
                .weight(0.20f)
                .fillMaxHeight()
        )

        // ── Column 2: shutter button / EV slider / lens switcher ────────────
        Column(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(GAP)
        ) {
            // Shutter button
            GlassCell(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                OriginShutterButton(
                    isRecording = isRecording,
                    onCapture = onCapture
                )
            }

            // EV slider
            GlassCell(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                EvSlider(
                    currentEvIndex = currentEvIndex,
                    onEvChange = onEvChange,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Lens switcher
            GlassCell(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LensSwitcher(
                    isUltraWide = isUltraWide,
                    onFlipLens = onFlipLens
                )
            }
        }

        // ── Column 3: zoom slider / P toggle / trackpad ─────────────────────
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(GAP)
        ) {
            // Zoom slider
            GlassCell(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ZoomScale(
                    currentZoomRatio = currentZoomRatio,
                    minZoomRatio = minZoomRatio,
                    maxZoomRatio = maxZoomRatio,
                    onZoomChange = onZoomChange,
                    modifier = Modifier.fillMaxSize(0.85f)
                )
            }

            // P / Video mode toggle
            GlassCell(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CaptureModeToggle(
                    isVideoMode = isVideoMode,
                    onToggle = onToggleCaptureMode
                )
            }

            // Trackpad (frosted white)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(CELL_SHAPE)
                    .background(TrackpadBg)
                    .border(CELL_BORDER, PanelBorder, CELL_SHAPE)
            ) {
                FocusTrackpad(
                    onFocusChange = onTrackpadFocusChange,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ── Shutter-speed strip ───────────────────────────────────────────────────────

@Composable
private fun ShutterSpeedStrip(
    currentSpeed: Int,
    onSpeedChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }

    // Find initial index from currentSpeed
    val selectedIndex = remember(currentSpeed) {
        SHUTTER_SPEEDS.indexOf(currentSpeed).coerceAtLeast(0)
    }

    Box(
        modifier = modifier
            .clip(CELL_SHAPE)
            .background(PanelBg)
            .border(CELL_BORDER, PanelBorder, CELL_SHAPE)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    dragAccum += dragAmount
                    val steps = (dragAccum / 40f).roundToInt()
                    if (steps != 0) {
                        val newIndex = (selectedIndex + steps)
                            .coerceIn(0, SHUTTER_SPEEDS.lastIndex)
                        if (newIndex != selectedIndex) {
                            onSpeedChange(SHUTTER_SPEEDS[newIndex])
                        }
                        dragAccum -= steps * 40f
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.End
        ) {
            val visibleRange = ((selectedIndex - 2)..(selectedIndex + 2))
                .filter { it in SHUTTER_SPEEDS.indices }

            visibleRange.forEach { idx ->
                val isSelected = idx == selectedIndex
                val distance = kotlin.math.abs(idx - selectedIndex)
                Text(
                    text = "${SHUTTER_SPEEDS[idx]}",
                    color = when {
                        isSelected -> OriginOrange
                        distance == 1 -> LightGray
                        else -> DimGray
                    },
                    fontSize = when {
                        isSelected -> 20.sp
                        distance == 1 -> 14.sp
                        else -> 11.sp
                    },
                    fontWeight = when {
                        isSelected -> FontWeight.Bold
                        distance == 1 -> FontWeight.Medium
                        else -> FontWeight.Normal
                    },
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp)
                )
            }
        }
    }
}

// ── EV slider ─────────────────────────────────────────────────────────────────

@Composable
private fun EvSlider(
    currentEvIndex: Int,
    onEvChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(currentEvIndex) {
                detectDragGestures { _, dragAmount ->
                    dragAccum += dragAmount.x
                    val steps = (dragAccum / 30f).roundToInt()
                    if (steps != 0) {
                        val newEv = (currentEvIndex + steps).coerceIn(-3, 3)
                        onEvChange(newEv)
                        dragAccum -= steps * 30f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // EV numeric label
            Text(
                text = "$currentEvIndex",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )

            // Small dot separator
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(LightGray)
            )

            // Equalizer: bars closest to orange = shortest, farthest = tallest
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(0.65f)
            ) {
                val canvasHeight = size.height
                val canvasWidth = size.width
                val barWidthPx = 4.dp.toPx()
                val gapPx = 3.dp.toPx()
                val barCount = 7
                val orangeIndex = (currentEvIndex + 3).coerceIn(0, 6)
                val totalBarsWidth = barCount * barWidthPx + (barCount - 1) * gapPx
                val startX = (canvasWidth - totalBarsWidth) / 2f
                val maxDist = maxOf(orangeIndex, barCount - 1 - orangeIndex).toFloat().coerceAtLeast(1f)

                for (i in 0 until barCount) {
                    val x = startX + i * (barWidthPx + gapPx)
                    val dist = abs(i - orangeIndex).toFloat()
                    val distRatio = (dist / maxDist).coerceIn(0f, 1f)

                    // Closest to orange = shortest, farthest = tallest
                    val barHeight = if (i == orangeIndex) {
                        canvasHeight * 0.20f
                    } else {
                        canvasHeight * (0.20f + distRatio * 0.70f)
                    }

                    val barColor = if (i == orangeIndex) OriginOrange
                    else LightGray.copy(alpha = 0.55f)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, (canvasHeight - barHeight) / 2f),
                        size = Size(barWidthPx, barHeight),
                        cornerRadius = CornerRadius(barWidthPx / 2f)
                    )
                }
            }
        }
    }
}

// ── Zoom scale (horizontal tick marks + vertical drag) ────────────────────────

@Composable
private fun ZoomScale(
    currentZoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }

    // Normalized position 0..1 (0 = min zoom, 1 = max zoom)
    val range = maxZoomRatio - minZoomRatio
    val normalizedPos = if (range > 0f) {
        ((currentZoomRatio - minZoomRatio) / range).coerceIn(0f, 1f)
    } else {
        0.5f
    }

    Box(
        modifier = modifier
            .pointerInput(minZoomRatio, maxZoomRatio) {
                detectDragGestures { _, dragAmount ->
                    dragAccum += dragAmount.y
                    val steps = (dragAccum / 20f).roundToInt()
                    if (steps != 0) {
                        // Dragging down = zoom in (higher ratio), dragging up = zoom out
                        val delta = -steps * (range / 20f)
                        val newZoom = (currentZoomRatio + delta)
                            .coerceIn(minZoomRatio, maxZoomRatio)
                        onZoomChange(newZoom)
                        dragAccum -= steps * 20f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            val centerX = size.width / 2f
            val tickHeight = 2.dp.toPx()
            val spacing = 5.dp.toPx()
            val tickCount = 9

            // orange bar position moves: top=0 (min zoom) to bottom=1 (max zoom)
            val orangeY = tickHeight + normalizedPos * (size.height - 2 * tickHeight)

            val orangeIndex = (normalizedPos * (tickCount - 1)).roundToInt()

            for (i in 0 until tickCount) {
                val barY = tickHeight + (i.toFloat() / (tickCount - 1)) * (size.height - 2 * tickHeight)
                val distFromOrange = abs(barY - orangeY)
                val maxDist = size.height / 2f
                val distRatio = (distFromOrange / maxDist).coerceIn(0f, 1f)

                val barWidth = size.width * (0.20f + distRatio * 0.80f)

                val barColor = if (i == orangeIndex) OriginOrange
                else LightGray.copy(alpha = (0.55f - distRatio * 0.4f).coerceIn(0.12f, 0.6f))

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(centerX - barWidth / 2f, barY - tickHeight / 2f),
                    size = Size(barWidth, tickHeight),
                    cornerRadius = CornerRadius(tickHeight / 2f)
                )
            }
        }
    }
}

// ── Lens switcher ─────────────────────────────────────────────────────────────

@Composable
private fun LensSwitcher(
    isUltraWide: Boolean,
    onFlipLens: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var rotationTarget by remember { mutableFloatStateOf(0f) }

    // Smoothly animate dial rotation
    val animatedRotation by animateFloatAsState(
        targetValue = rotationTarget,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "dialRotation"
    )

    // Rotate 180° each time isUltraWide changes
    LaunchedEffect(isUltraWide) {
        rotationTarget += 180f
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        dragAccum += dragAmount.y
                        // Each 60px of drag flips once, then resets so you can keep dragging
                        if (abs(dragAccum) > 60f) {
                            onFlipLens()
                            dragAccum = 0f
                        }
                    },
                    onDragEnd = { dragAccum = 0f }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // White dial circle — rotates on lens flip
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { rotationZ = animatedRotation }
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // Small dot indicator at top
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .offset(y = -(22).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF444444))
            )
        }
    }
}

// ── Shutter / record button ───────────────────────────────────────────────────

@Composable
private fun OriginShutterButton(
    isRecording: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { onCapture() }
        },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(if (isRecording) Color.Red else OriginOrange)
        )
    }
}

// ── Focus trackpad ────────────────────────────────────────────────────────────

@Composable
private fun FocusTrackpad(
    onFocusChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dotX by remember { mutableFloatStateOf(0.5f) }
    var dotY by remember { mutableFloatStateOf(0.5f) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val rx = (change.position.x / size.width).coerceIn(0f, 1f)
                    val ry = (change.position.y / size.height).coerceIn(0f, 1f)
                    dotX = rx
                    dotY = ry
                    onFocusChange(rx, ry)
                }
            }
    ) {
        val cx = size.width * dotX
        val cy = size.height * dotY
        val radius = 6.dp.toPx()

        // Orange focus dot
        drawCircle(
            color = OriginOrange,
            radius = radius,
            center = Offset(cx, cy)
        )
        // Border ring
        drawCircle(
            color = Color.DarkGray,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

// ── P / Video mode toggle ─────────────────────────────────────────────────────

@Composable
private fun CaptureModeToggle(
    isVideoMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { onToggle() }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Color(0xFF555555), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isVideoMode) "V" else "P",
                color = Color.Black,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif
            )
        }
    }
}

// ── Compose Preview ───────────────────────────────────────────────────────────

@Preview(widthDp = 720, heightDp = 360, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CaptureLayoutPreview() {
    PreviewLayout(
        viewfinder = { modifier ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFF2D4A2D))
            )
        },
        aspectRatioLabel = "3:4",
        lensLabel = "23 MM",
        currentEvIndex = -1,
        onEvChange = {},
        isRecording = false,
        isVideoMode = false
    )
}
