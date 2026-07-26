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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origin.browser.R
import com.origin.browser.data.model.TabData
import com.origin.browser.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(
    tabList: List<TabData>,
    onSelectTab: (Long) -> Unit,
    onCloseTab: (Long) -> Unit,
    onClearAllTabs: () -> Unit,
    onAddTab: () -> Unit,
    onClose: () -> Unit
) {
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
            // Header bar with pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tabs",
                    fontFamily = ntype82,
                    color = OriginWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Tab list
            if (tabList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No open tabs",
                        color = OriginMutedText,
                        fontSize = 15.sp
                    )
                }
            } else {
                val scope = rememberCoroutineScope()
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(tabList, key = { it.id }) { tab ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    scope.launch { onCloseTab(tab.id) }
                                    true
                                } else {
                                    false
                                }
                            },
                            positionalThreshold = { totalDistance -> totalDistance * 0.5f }
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
                                        .background(color, RoundedCornerShape(16.dp))
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text("Close", color = OriginWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(OriginSurface)
                                    .border(1.dp, OriginOutline, RoundedCornerShape(16.dp))
                                    .clickable { onSelectTab(tab.id) }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Preview area
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .background(OriginDarkBackground)
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(OriginOutline),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (tabList.indexOf(tab) + 1).toString(),
                                                    color = OriginWhite,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = tab.url,
                                                color = OriginMutedText,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    // Title bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = tab.title.ifBlank { tab.url },
                                            color = OriginWhite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== FLOATING BOTTOM NAVIGATION BAR ====================
        var pillVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { pillVisible = true }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
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
                    if (tabList.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAllTabs,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_clear_tabs),
                                contentDescription = "Clear All Tabs",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onAddTab,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_add_tab),
                            contentDescription = "Add Tab",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                }
            }
        }
    }
}
