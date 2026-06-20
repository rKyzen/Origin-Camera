package com.google.jetpackcamera.ui.components.capture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.ColorScienceMode
import com.google.jetpackcamera.model.FlashMode
import kotlin.math.roundToInt

const val QUICK_CONTROLS_PANEL_TAG = "quick_controls_panel"
const val QUICK_CONTROLS_EV_SLIDER_TAG = "quick_controls_ev_slider"
const val QUICK_CONTROLS_MFS_TOGGLE_TAG = "quick_controls_mfs_toggle"
const val QUICK_CONTROLS_COLOR_SCIENCE_SELECTOR_TAG = "quick_controls_color_science_selector"
const val QUICK_CONTROLS_ASPECT_RATIO_SELECTOR_TAG = "quick_controls_aspect_ratio_selector"
const val QUICK_CONTROLS_FLASH_SELECTOR_TAG = "quick_controls_flash_selector"
const val FILTER_PANEL_TAG = "filter_panel"
const val FILTER_PRESET_SELECTOR_TAG = "filter_preset_selector"

private val TrayBackground = Color.Black
private val TraySurface = Color(0xFF1A1A1A)
private val TrayOnSurface = Color(0xFFE0E0E0)
private val TrayMuted = Color(0xFF888888)
private val TrayAccent = Color.White
private val TrayBorder = Color(0xFF2A2A2A)
private val TrayDivider = Color(0xFF1A1A1A)

@Composable
fun QuickControlsTray(
    modifier: Modifier = Modifier,
    visible: Boolean,
    currentEvIndex: Int,
    onEvChange: (Int) -> Unit,
    isMultiFrameStackingEnabled: Boolean,
    onMfsToggle: (Boolean) -> Unit,
    colorScienceMode: ColorScienceMode,
    onColorScienceModeChange: (ColorScienceMode) -> Unit,
    currentAspectRatio: AspectRatio,
    onAspectRatioChange: (AspectRatio) -> Unit,
    currentFlashMode: FlashMode,
    onFlashModeChange: (FlashMode) -> Unit
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = expandVertically(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 300,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 200,
                delayMillis = 100
            )
        ),
        exit = shrinkVertically(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 250,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 150)
        )
    ) {
        QuickControlsContent(
            currentEvIndex = currentEvIndex,
            onEvChange = onEvChange,
            isMultiFrameStackingEnabled = isMultiFrameStackingEnabled,
            onMfsToggle = onMfsToggle,
            colorScienceMode = colorScienceMode,
            onColorScienceModeChange = onColorScienceModeChange,
            currentAspectRatio = currentAspectRatio,
            onAspectRatioChange = onAspectRatioChange,
            currentFlashMode = currentFlashMode,
            onFlashModeChange = onFlashModeChange
        )
    }
}

@Composable
private fun QuickControlsContent(
    currentEvIndex: Int,
    onEvChange: (Int) -> Unit,
    isMultiFrameStackingEnabled: Boolean,
    onMfsToggle: (Boolean) -> Unit,
    colorScienceMode: ColorScienceMode,
    onColorScienceModeChange: (ColorScienceMode) -> Unit,
    currentAspectRatio: AspectRatio,
    onAspectRatioChange: (AspectRatio) -> Unit,
    currentFlashMode: FlashMode,
    onFlashModeChange: (FlashMode) -> Unit
) {
    var localEvIndex by remember { mutableFloatStateOf(currentEvIndex.toFloat()) }
    var localMfsEnabled by remember { mutableStateOf(isMultiFrameStackingEnabled) }
    var localColorScience by remember { mutableStateOf(colorScienceMode) }
    var localAspectRatio by remember { mutableStateOf(currentAspectRatio) }
    var localFlashMode by remember { mutableStateOf(currentFlashMode) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TrayBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── 1. Exposure Compensation ───────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.quick_controls_ev_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TrayOnSurface
                )
                Text(
                    text = formatEvValue(localEvIndex.roundToInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TrayAccent
                )
            }
            Slider(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { testTag = QUICK_CONTROLS_EV_SLIDER_TAG },
                value = localEvIndex,
                onValueChange = {
                    localEvIndex = it
                    onEvChange(it.roundToInt())
                },
                valueRange = -4f..4f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = TrayAccent,
                    activeTrackColor = TrayAccent,
                    inactiveTrackColor = TrayBorder,
                    inactiveTickColor = TrayBorder
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.quick_controls_ev_min),
                    style = MaterialTheme.typography.labelSmall,
                    color = TrayMuted
                )
                Text(
                    text = stringResource(R.string.quick_controls_ev_max),
                    style = MaterialTheme.typography.labelSmall,
                    color = TrayMuted
                )
            }
        }

        // Divider
        TrayDivider()

        // ── 2. MFS Toggle ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = QUICK_CONTROLS_MFS_TOGGLE_TAG },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quick_controls_mfs_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TrayOnSurface
                )
                Text(
                    text = stringResource(R.string.quick_controls_mfs_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = TrayMuted
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            TraySwitch(
                checked = localMfsEnabled,
                onCheckedChange = {
                    localMfsEnabled = it
                    onMfsToggle(it)
                }
            )
        }

        // Divider
        TrayDivider()

        // ── 3. Color Science ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = QUICK_CONTROLS_COLOR_SCIENCE_SELECTOR_TAG },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_controls_color_science_label),
                style = MaterialTheme.typography.bodyLarge,
                color = TrayOnSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrayChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.quick_controls_color_science_off),
                    selected = localColorScience == ColorScienceMode.OFF,
                    onClick = {
                        localColorScience = ColorScienceMode.OFF
                        onColorScienceModeChange(ColorScienceMode.OFF)
                    }
                )
                TrayChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.quick_controls_color_science_auto),
                    selected = localColorScience == ColorScienceMode.AUTO_TUNED,
                    onClick = {
                        localColorScience = ColorScienceMode.AUTO_TUNED
                        onColorScienceModeChange(ColorScienceMode.AUTO_TUNED)
                    }
                )
            }
        }

        // Divider
        TrayDivider()

        // ── 4. Frame Ratio ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = QUICK_CONTROLS_ASPECT_RATIO_SELECTOR_TAG },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_controls_aspect_ratio_label),
                style = MaterialTheme.typography.bodyLarge,
                color = TrayOnSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrayChip(
                    modifier = Modifier.weight(1f),
                    label = "4:3",
                    selected = localAspectRatio == AspectRatio.THREE_FOUR,
                    onClick = {
                        localAspectRatio = AspectRatio.THREE_FOUR
                        onAspectRatioChange(AspectRatio.THREE_FOUR)
                    }
                )
                TrayChip(
                    modifier = Modifier.weight(1f),
                    label = "1:1",
                    selected = localAspectRatio == AspectRatio.ONE_ONE,
                    onClick = {
                        localAspectRatio = AspectRatio.ONE_ONE
                        onAspectRatioChange(AspectRatio.ONE_ONE)
                    }
                )
                TrayChip(
                    modifier = Modifier.weight(1f),
                    label = "16:9",
                    selected = localAspectRatio == AspectRatio.NINE_SIXTEEN,
                    onClick = {
                        localAspectRatio = AspectRatio.NINE_SIXTEEN
                        onAspectRatioChange(AspectRatio.NINE_SIXTEEN)
                    }
                )
            }
        }

        // Divider
        TrayDivider()

        // ── 5. Flash ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = QUICK_CONTROLS_FLASH_SELECTOR_TAG },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_controls_flash_label),
                style = MaterialTheme.typography.bodyLarge,
                color = TrayOnSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlashChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.quick_settings_flash_auto),
                    iconRes = R.drawable.ic_flash_auto_filled,
                    selected = localFlashMode == FlashMode.AUTO,
                    onClick = {
                        localFlashMode = FlashMode.AUTO
                        onFlashModeChange(FlashMode.AUTO)
                    }
                )
                FlashChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.quick_settings_flash_on),
                    iconRes = R.drawable.ic_flash_on_filled,
                    selected = localFlashMode == FlashMode.ON,
                    onClick = {
                        localFlashMode = FlashMode.ON
                        onFlashModeChange(FlashMode.ON)
                    }
                )
                FlashChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.quick_settings_flash_off),
                    iconRes = R.drawable.ic_flash_off,
                    selected = localFlashMode == FlashMode.OFF,
                    onClick = {
                        localFlashMode = FlashMode.OFF
                        onFlashModeChange(FlashMode.OFF)
                    }
                )
            }
        }
    }
}

@Composable
private fun TraySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = TrayAccent,
            checkedTrackColor = TrayAccent.copy(alpha = 0.25f),
            uncheckedThumbColor = TrayMuted,
            uncheckedTrackColor = TrayBorder,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun TrayChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) TrayAccent else TraySurface
    val contentColor = if (selected) Color.Black else TrayMuted
    val borderStroke = if (selected) {
        BorderStroke(1.dp, TrayAccent)
    } else {
        BorderStroke(1.dp, TrayBorder)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        contentColor = contentColor,
        border = borderStroke,
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun FlashChip(
    modifier: Modifier = Modifier,
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) TrayAccent else TraySurface
    val contentColor = if (selected) Color.Black else TrayMuted
    val borderStroke = if (selected) {
        BorderStroke(1.dp, TrayAccent)
    } else {
        BorderStroke(1.dp, TrayBorder)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        contentColor = contentColor,
        border = borderStroke,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(contentColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TrayDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(TrayDivider)
    )
}

private fun formatEvValue(ev: Int): String {
    return when {
        ev > 0 -> "+$ev.0"
        ev == 0 -> "0.0"
        else -> "$ev.0"
    }
}

@Composable
fun FilterPanel(
    modifier: Modifier = Modifier,
    visible: Boolean,
    selectedFilter: com.google.jetpackcamera.model.FilterPreset,
    onFilterSelected: (com.google.jetpackcamera.model.FilterPreset) -> Unit
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = expandVertically(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 300,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 200,
                delayMillis = 100
            )
        ),
        exit = shrinkVertically(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 250,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 150)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TrayBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics {
                    testTag = FILTER_PANEL_TAG
                    testTagsAsResourceId = true
                },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        testTag = FILTER_PRESET_SELECTOR_TAG
                        testTagsAsResourceId = true
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.google.jetpackcamera.model.FilterPreset.entries.forEach { preset ->
                    TrayChip(
                        modifier = Modifier.weight(1f),
                        label = preset.label,
                        selected = preset == selectedFilter,
                        onClick = { onFilterSelected(preset) }
                    )
                }
            }
        }
    }
}
