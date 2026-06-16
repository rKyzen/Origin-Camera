package com.google.jetpackcamera.ui.components.capture

import android.view.OrientationEventListener.ORIENTATION_UNKNOWN
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBg = Color(0xFF1C1C1C)
private val CardBorder = Color(0x22FFFFFF)
private val ThumbnailBorder = Color(0x88FFFFFF)
private val ButtonBg = Color.White
private val ButtonText = Color(0xFF0A0A0A)

data class FilterItem(
    val title: String,
    val subtitle: String,
    val thumbnailColor: Color
)

@Composable
fun FiltersScreen(
    filters: List<FilterItem>,
    onFilterClick: (Int) -> Unit = { _ -> },
    onAddFilter: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onFiltersClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewfinder: @Composable (Modifier) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Viewfinder + brackets + level ────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(36.dp))
        ) {
            // Camera preview
            viewfinder(Modifier.fillMaxSize())

            // Level indicator — exact copy from CaptureLayout
            var deviceOrientation by remember { mutableIntStateOf(ORIENTATION_UNKNOWN) }
            val context = LocalContext.current
            androidx.compose.runtime.LaunchedEffect(Unit) {
                rawOrientationDegreesFlow(context).collect { deviceOrientation = it }
            }
            LevelIndicator(
                deviceOrientation = deviceOrientation,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 160.dp, height = 36.dp)
            )

            // Corner brackets — exact copy from CaptureLayout
            CornerBrackets(modifier = Modifier.fillMaxSize())
        }

        // ── Filter cards ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(top = 4.dp)
        ) {
            filters.forEachIndexed { index, filter ->
                FilterCard(
                    filter = filter,
                    onClick = { onFilterClick(index) }
                )
                if (index < filters.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Add filter button — white pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ButtonBg)
                    .clickable(onClick = onAddFilter)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add Color Filter",
                    color = ButtonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Bottom toolbar ────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            BottomToolbar(
                modifier = Modifier.padding(bottom = 8.dp),
                onGalleryClick = onGalleryClick,
                onFiltersClick = onFiltersClick,
                onSettingsClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun FilterCard(
    filter: FilterItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(0.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = filter.title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = filter.subtitle,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, ThumbnailBorder, RoundedCornerShape(10.dp))
                .background(filter.thumbnailColor)
        )
    }
}

@Preview(widthDp = 380, heightDp = 720, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FiltersScreenPreview() {
    FiltersScreen(
        filters = listOf(
            FilterItem("Green Vibes", "Cinematic\ngreen skies", Color(0xFF4A7A6A)),
            FilterItem("Retro Field", "Color Filter for\ndigital warmth", Color(0xFF6A5A3A)),
        )
    )
}
