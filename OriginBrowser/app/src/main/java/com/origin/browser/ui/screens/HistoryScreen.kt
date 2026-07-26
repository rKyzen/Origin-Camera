package com.origin.browser.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origin.browser.R
import com.origin.browser.data.db.entity.HistoryEntry
import com.origin.browser.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    historyList: List<HistoryEntry>,
    onSelectUrl: (String) -> Unit,
    onClearAll: () -> Unit,
    onDeleteEntry: (HistoryEntry) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = remember(historyList, searchQuery) {
        if (searchQuery.isBlank()) {
            historyList
        } else {
            historyList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.url.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val ntype82 = FontFamily(Font(R.font.ntype82_regular))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OriginDarkBackground.copy(alpha = 0.95f))
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "History",
                    fontFamily = ntype82,
                    color = OriginWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Search filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter history...", color = OriginMutedText) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OriginWhite,
                    unfocusedBorderColor = OriginOutline,
                    focusedTextColor = OriginWhite,
                    unfocusedTextColor = OriginWhite,
                    focusedContainerColor = OriginSurface,
                    unfocusedContainerColor = OriginSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No history recorded yet" else "No matching history found",
                        color = OriginMutedText,
                        fontSize = 15.sp
                    )
                }
            } else {
                val scope = rememberCoroutineScope()
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    scope.launch {
                                        onDeleteEntry(item)
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
                            positionalThreshold = { totalDistance -> totalDistance * 0.6f }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = Color(0xFFFF3B30),
                                    label = "swipeBg"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(12.dp))
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text("Delete", color = OriginWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(OriginSurface, RoundedCornerShape(12.dp))
                                    .border(1.dp, OriginOutline, RoundedCornerShape(12.dp))
                                    .clickable { onSelectUrl(item.url) }
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = item.title.ifBlank { item.url },
                                    color = OriginWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.url,
                                        color = OriginMutedText,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = dateFormat.format(Date(item.timestamp)),
                                        color = OriginMutedText.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom pill
        var pillVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { pillVisible = true }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            AnimatedVisibility(
                visible = pillVisible,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.6f, stiffness = 260f)) + scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 260f)) + fadeIn(animationSpec = spring(dampingRatio = 0.6f, stiffness = 260f)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(dampingRatio = 0.6f, stiffness = 260f)) + scaleOut(targetScale = 0.85f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 260f)) + fadeOut(animationSpec = spring(dampingRatio = 0.6f, stiffness = 260f))
            ) {
            Surface(
                color = OriginDarkBackground.copy(alpha = 0.75f),
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, OriginWhite, CircleShape)
                    .wrapContentSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Close",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    if (historyList.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_clear_history),
                                contentDescription = "Clear All History",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                }
            }
        }
    }
}
