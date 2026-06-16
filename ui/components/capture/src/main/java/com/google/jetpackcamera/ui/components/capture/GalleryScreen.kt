package com.google.jetpackcamera.ui.components.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenBg = Color.Black
private val CardBorder = Color(0x33FFFFFF)

data class GalleryItem(
    val label: String,
    val thumbnailColor: Color,
    val date: String = ""
)

@Composable
fun GalleryScreen(
    items: List<GalleryItem>,
    onItemClick: (Int) -> Unit = { _ -> },
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gallery",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Grid of photos
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items.size) { index ->
                GalleryThumbnail(
                    item = items[index],
                    onClick = { onItemClick(index) }
                )
            }
        }

        // Bottom toolbar
        BottomToolbar(modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun GalleryThumbnail(
    item: GalleryItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(item.thumbnailColor)
            .border(0.5.dp, CardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart
    ) {
        if (item.date.isNotEmpty()) {
            Text(
                text = item.date,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Preview(widthDp = 380, heightDp = 720, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GalleryScreenPreview() {
    GalleryScreen(
        items = listOf(
            GalleryItem("Photo 1", Color(0xFF5B8A72), "Jun 15"),
            GalleryItem("Photo 2", Color(0xFF8B6E4E), "Jun 15"),
            GalleryItem("Photo 3", Color(0xFF6B6B6B), "Jun 14"),
            GalleryItem("Photo 4", Color(0xFF5A7A8A), "Jun 14"),
            GalleryItem("Photo 5", Color(0xFF7A5A5A), "Jun 13"),
            GalleryItem("Photo 6", Color(0xFF5A8A5A), "Jun 13"),
        )
    )
}
