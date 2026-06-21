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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

private val ToolbarBg = Color(0xCC1A1A1A)
private val PillBg = Color(0xE6FFFFFF)
private val ZoomPillBg = Color(0x99333333)
private val FrostedPill = Color(0xCC2A2A2A)
private val BlurPillBg = Color(0x442A2A2A)

val funnelSansFamily = FontFamily(
    Font(R.font.funnel_sans_regular, FontWeight.Normal),
    Font(R.font.funnel_sans_medium, FontWeight.Medium),
    Font(R.font.funnel_sans_semi_bold, FontWeight.SemiBold),
    Font(R.font.funnel_sans_bold, FontWeight.Bold)
)

@Composable
fun PreviewLayout(
    modifier: Modifier = Modifier,
    viewfinder: @Composable (Modifier) -> Unit,
    captureButton: @Composable (Modifier) -> Unit = {},
    imageWell: @Composable (Modifier) -> Unit = {},
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
    viewfinderAspectRatio: Float = 3f / 4f,
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
    onEffectsClick: () -> Unit = {},
    deviceOrientation: Int = ORIENTATION_UNKNOWN,
    isQuickControlsExpanded: Boolean = false,
    quickControlsTray: @Composable () -> Unit = {},
    onShutterTap: () -> Unit = onCapture,
    onBackClick: () -> Unit = {},
    focusMeteringUiState: com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState = com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState.Unspecified,
    coordinateTransformer: androidx.camera.viewfinder.compose.CoordinateTransformer = androidx.camera.viewfinder.compose.MutableCoordinateTransformer()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val viewfinderHeightFraction by animateFloatAsState(
        targetValue = if (isQuickControlsExpanded) 0.45f else 0.80f,
        animationSpec = tween(
            durationMillis = 300,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "viewfinderHeight"
    )

    Box(modifier = modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Viewfinder: fixed container, aspect ratio inside ──────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .fillMaxHeight(viewfinderHeightFraction)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color.Black)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(viewfinderAspectRatio)
                        .align(Alignment.Center)
                ) {
                    viewfinder(Modifier.fillMaxSize())
                }

                Text(
                    text = "$aspectRatioLabel  $lensLabel",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = funnelSansFamily,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 50.dp)
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

                // ── Corner brackets inside viewfinder ────────────────────
                CornerBrackets(
                    modifier = Modifier.fillMaxSize(),
                    focusMeteringUiState = focusMeteringUiState,
                    coordinateTransformer = coordinateTransformer
                )

                // ── Level indicator (rotates with device tilt) ─────────
                LevelIndicator(
                    deviceOrientation = deviceOrientation,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.8f)
                        .height(54.dp)
                )
            }

            // ── Shutter / Back toggle ──────────────────────────────────
            Box(
                modifier = Modifier
                    .let { if (isQuickControlsExpanded) it else it.offset(y = (-39).dp) }
                    .size(width = 132.dp, height = 86.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 132.dp, height = 86.dp)
                        .clip(RoundedCornerShape(43.dp))
                        .background(BlurPillBg)
                        .blur(20.dp)
                )
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 62.dp)
                        .shadow(8.dp, RoundedCornerShape(31.dp))
                        .clip(RoundedCornerShape(31.dp))
                        .background(if (isQuickControlsExpanded) Color(0xFF2A2A2A) else PillBg)
                        .clickable { onShutterTap() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isQuickControlsExpanded) {
                        Image(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
            }

            // ── Quick Controls Tray ──────────────────────────────────
            quickControlsTray()

            // ── Controls: remaining space ─────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                BottomToolbar(
                    modifier = Modifier.padding(top = 8.dp),
                    onGalleryClick = onGalleryClick,
                    onFiltersClick = onFiltersClick,
                    onEffectsClick = onEffectsClick
                )
            }
        }

        quickSettingsOverlay(Modifier)
        debugOverlay(Modifier)
    }
}

@Composable
fun CornerBrackets(
    modifier: Modifier = Modifier,
    focusMeteringUiState: com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState = com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState.Unspecified,
    coordinateTransformer: androidx.camera.viewfinder.compose.CoordinateTransformer = androidx.camera.viewfinder.compose.MutableCoordinateTransformer()
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val bracketSizePx = with(density) { 48.dp.toPx() }
    val focusBoxHalfPx = with(density) { 40.dp.toPx() }
    val padX = with(density) { 20.dp.toPx() }
    val padY = with(density) { 12.dp.toPx() }

    var containerW by remember { mutableFloatStateOf(0f) }
    var containerH by remember { mutableFloatStateOf(0f) }

    val tlX = remember { Animatable(0f) }
    val tlY = remember { Animatable(0f) }
    val trX = remember { Animatable(0f) }
    val trY = remember { Animatable(0f) }
    val blX = remember { Animatable(0f) }
    val blY = remember { Animatable(0f) }
    val brX = remember { Animatable(0f) }
    val brY = remember { Animatable(0f) }

    val defaultTl = remember(containerW, containerH) { Offset(padX, padY) }
    val defaultTr = remember(containerW, containerH) { Offset(containerW - padX - bracketSizePx, padY) }
    val defaultBl = remember(containerW, containerH) { Offset(padX, containerH - padY - bracketSizePx) }
    val defaultBr = remember(containerW, containerH) { Offset(containerW - padX - bracketSizePx, containerH - padY - bracketSizePx) }

    val animSpec = tween<Float>(200, easing = EaseOutExpo)

    val status = if (focusMeteringUiState is com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState.Specified)
        focusMeteringUiState.status else null

    var showResult by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        when (status) {
            com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState.Status.SUCCESS,
            com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState.Status.FAILURE -> {
                showResult = true
                delay(500L)
                showResult = false
            }
            else -> showResult = false
        }
    }

    LaunchedEffect(focusMeteringUiState, containerW, containerH) {
        if (focusMeteringUiState !is com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState.Specified ||
            containerW == 0f || containerH == 0f) {
            launch { tlX.animateTo(defaultTl.x, animSpec) }
            launch { tlY.animateTo(defaultTl.y, animSpec) }
            launch { trX.animateTo(defaultTr.x, animSpec) }
            launch { trY.animateTo(defaultTr.y, animSpec) }
            launch { blX.animateTo(defaultBl.x, animSpec) }
            launch { blY.animateTo(defaultBl.y, animSpec) }
            launch { brX.animateTo(defaultBr.x, animSpec) }
            launch { brY.animateTo(defaultBr.y, animSpec) }
            return@LaunchedEffect
        }

        val screenCoords = Matrix().run {
            setFrom(coordinateTransformer.transformMatrix)
            invert()
            map(focusMeteringUiState.surfaceCoordinates)
        }

        val focusCx = screenCoords.x
        val focusCy = screenCoords.y

        val focusTl = Offset(focusCx - focusBoxHalfPx, focusCy - focusBoxHalfPx)
        val focusTr = Offset(focusCx + focusBoxHalfPx - bracketSizePx, focusCy - focusBoxHalfPx)
        val focusBl = Offset(focusCx - focusBoxHalfPx, focusCy + focusBoxHalfPx - bracketSizePx)
        val focusBr = Offset(focusCx + focusBoxHalfPx - bracketSizePx, focusCy + focusBoxHalfPx - bracketSizePx)

        launch { tlX.animateTo(focusTl.x, animSpec) }
        launch { tlY.animateTo(focusTl.y, animSpec) }
        launch { trX.animateTo(focusTr.x, animSpec) }
        launch { trY.animateTo(focusTr.y, animSpec) }
        launch { blX.animateTo(focusBl.x, animSpec) }
        launch { blY.animateTo(focusBl.y, animSpec) }
        launch { brX.animateTo(focusBr.x, animSpec) }
        launch { brY.animateTo(focusBr.y, animSpec) }
    }

    val bracketColor = when {
        status == com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState.Status.SUCCESS -> Color(0xFF4CAF50)
        status == com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState.Status.FAILURE -> Color(0xFFF44336)
        else -> Color.White
    }

    val bracketAlpha = when {
        focusMeteringUiState is com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState.Unspecified -> 1f
        showResult -> 0.6f
        else -> 1f
    }

    Box(modifier = modifier.onSizeChanged { containerW = it.width.toFloat(); containerH = it.height.toFloat() }) {
        Image(
            painter = painterResource(R.drawable.ic_bracket_top_left),
            contentDescription = null,
            colorFilter = ColorFilter.tint(bracketColor),
            modifier = Modifier
                .size(48.dp)
                .offset { IntOffset(tlX.value.toInt(), tlY.value.toInt()) }
                .graphicsLayer { alpha = bracketAlpha }
        )
        Image(
            painter = painterResource(R.drawable.ic_bracket_top_right),
            contentDescription = null,
            colorFilter = ColorFilter.tint(bracketColor),
            modifier = Modifier
                .size(48.dp)
                .offset { IntOffset(trX.value.toInt(), trY.value.toInt()) }
                .graphicsLayer { alpha = bracketAlpha }
        )
        Image(
            painter = painterResource(R.drawable.ic_bracket_bottom_left),
            contentDescription = null,
            colorFilter = ColorFilter.tint(bracketColor),
            modifier = Modifier
                .size(48.dp)
                .offset { IntOffset(blX.value.toInt(), blY.value.toInt()) }
                .graphicsLayer { alpha = bracketAlpha }
        )
        Image(
            painter = painterResource(R.drawable.ic_bracket_bottom_right),
            contentDescription = null,
            colorFilter = ColorFilter.tint(bracketColor),
            modifier = Modifier
                .size(48.dp)
                .offset { IntOffset(brX.value.toInt(), brY.value.toInt()) }
                .graphicsLayer { alpha = bracketAlpha }
        )
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
    val tintColor = if (isLevel) Color(0xFFFFD700) else Color.White.copy(alpha = 0.55f)

    Image(
        painter = painterResource(R.drawable.ic_bracket_middle),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tintColor),
        modifier = modifier.graphicsLayer { rotationZ = animatedTilt }
    )
}

@Composable
private fun HorizontalZoomSlider(
    currentZoomRatio: Float, minZoomRatio: Float, maxZoomRatio: Float,
    onZoomChange: (Float) -> Unit, modifier: Modifier = Modifier
) {
    val logMin = ln(minZoomRatio)
    val logRange = ln(maxZoomRatio / minZoomRatio)
    val normalizedPos = if (logRange > 0f) (ln(currentZoomRatio) - logMin) / logRange else 0.5f
    val zoomLabel = if (currentZoomRatio <= 1.05f) "1x"
    else if (currentZoomRatio < 10f) "${"%.1f".format(currentZoomRatio)}x"
    else "${currentZoomRatio.roundToInt()}x"

    val currentOnZoomChange by rememberUpdatedState(onZoomChange)
    val smoothPos by animateFloatAsState(
        targetValue = normalizedPos,
        animationSpec = tween(120)
    )

    Box(modifier = modifier
            .pointerInput(minZoomRatio, maxZoomRatio) {
                detectDragGestures(onDrag = { change, _ ->
                    val norm = (change.position.x / size.width).coerceIn(0f, 1f)
                    currentOnZoomChange(exp(logMin + norm * logRange))
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val cy = h / 2f
            val tickCount = 25
            val visibleW = w
            val totalRange = visibleW * 1.6f
            val spacing = totalRange / (tickCount - 1)
            val startX = (visibleW - totalRange) / 2f - smoothPos * (totalRange - visibleW)

            for (i in 0 until tickCount) {
                val x = startX + i * spacing
                val major = i % 5 == 0
                val th = if (major) 14.dp.toPx() else 8.dp.toPx()
                val tw = if (major) 2.dp.toPx() else 1.dp.toPx()
                val color = if (major) Color.White.copy(0.7f) else Color.White.copy(0.35f)
                if (x + tw / 2f >= 0 && x - tw / 2f <= visibleW) {
                    drawRoundRect(color, Offset(x - tw / 2f, cy - th / 2f), Size(tw, th), CornerRadius(tw / 2f))
                }
            }
        }
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(ZoomPillBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = zoomLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = funnelSansFamily
            )
        }
    }
}

// toolbar icons
@Composable
fun BottomToolbar(
    modifier: Modifier = Modifier,
    onGalleryClick: () -> Unit = {},
    onFiltersClick: () -> Unit = {},
    onEffectsClick: () -> Unit = {}
) {
    Row(modifier = modifier
            .clip(RoundedCornerShape(20.dp)).background(ToolbarBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_toolbar_gallery),
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .clickable(onClick = onGalleryClick)
        )
        Image(
            painter = painterResource(R.drawable.ic_toolbar_filters),
            contentDescription = null,
            modifier = Modifier
                .size(35.dp)
                .clickable(onClick = onFiltersClick)
        )
        Image(
            painter = painterResource(R.drawable.ic_toolbar_effects),
            contentDescription = null,
            modifier = Modifier
                .size(35.dp)
                .clickable(onClick = onEffectsClick)
        )
    }
}

@Preview(widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CaptureLayoutPreview() {
        PreviewLayout(
        viewfinder = { m -> Box(m.fillMaxSize().background(Color(0xFF2D4A2D))) },
        aspectRatioLabel = "9:12", lensLabel = "23mm",
        currentEvIndex = 0, onEvChange = {}, isRecording = false, isVideoMode = false,
        onEffectsClick = {}
    )
}
