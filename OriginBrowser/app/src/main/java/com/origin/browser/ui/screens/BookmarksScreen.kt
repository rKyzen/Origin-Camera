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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origin.browser.R
import com.origin.browser.data.db.entity.BookmarkEntry
import com.origin.browser.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    bookmarkList: List<BookmarkEntry>,
    currentUrl: String,
    currentTitle: String,
    isCurrentPageBookmarked: Boolean,
    onAddBookmark: (String, String) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onUpdateBookmark: (Long, String) -> Unit,
    onSelectUrl: (String) -> Unit,
    onClose: () -> Unit
) {
    val ntype82 = FontFamily(Font(R.font.ntype82_regular))
    var showAddCustom by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }

    // Edit dialog state
    var editItem by remember { mutableStateOf<BookmarkEntry?>(null) }
    var editTitle by remember { mutableStateOf("") }

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
                    text = "Bookmarks",
                    fontFamily = ntype82,
                    color = OriginWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick Add button for current page
            if (currentUrl.isNotBlank()) {
                Button(
                    onClick = {
                        if (isCurrentPageBookmarked) {
                            onRemoveBookmark(currentUrl)
                        } else {
                            onAddBookmark(currentUrl, currentTitle.ifBlank { currentUrl })
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentPageBookmarked) OriginSurface else OriginWhite,
                        contentColor = if (isCurrentPageBookmarked) Color(0xFFFF6B6B) else OriginBlack
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = if (isCurrentPageBookmarked) "★ Remove Current Page" else "+ Bookmark Current Page",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Add Custom Bookmark toggle + form
            TextButton(
                onClick = { showAddCustom = !showAddCustom },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    if (showAddCustom) "▾ Cancel" else "+ Add Custom Bookmark",
                    color = OriginOutlineHighlight,
                    fontSize = 13.sp
                )
            }

            AnimatedVisibility(visible = showAddCustom) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OriginSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, OriginOutline, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        placeholder = { Text("URL", color = OriginMutedText, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OriginWhite,
                            unfocusedBorderColor = OriginOutline,
                            focusedTextColor = OriginWhite,
                            unfocusedTextColor = OriginWhite,
                            focusedContainerColor = OriginDarkBackground,
                            unfocusedContainerColor = OriginDarkBackground,
                            cursorColor = OriginWhite
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        placeholder = { Text("Title (optional)", color = OriginMutedText, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OriginWhite,
                            unfocusedBorderColor = OriginOutline,
                            focusedTextColor = OriginWhite,
                            unfocusedTextColor = OriginWhite,
                            focusedContainerColor = OriginDarkBackground,
                            unfocusedContainerColor = OriginDarkBackground,
                            cursorColor = OriginWhite
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val url = customUrl.trim()
                            if (url.isNotBlank()) {
                                val formatted = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                                onAddBookmark(formatted, customTitle.trim().ifBlank { formatted })
                                customUrl = ""
                                customTitle = ""
                                showAddCustom = false
                            }
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val url = customUrl.trim()
                            if (url.isNotBlank()) {
                                val formatted = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                                onAddBookmark(formatted, customTitle.trim().ifBlank { formatted })
                                customUrl = ""
                                customTitle = ""
                                showAddCustom = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OriginWhite,
                            contentColor = OriginBlack
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Bookmark", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            // Bookmark list
            if (bookmarkList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved bookmarks",
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
                    items(bookmarkList, key = { it.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        scope.launch { onDeleteBookmark(item.id) }
                                        true
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        editItem = item
                                        editTitle = item.title
                                        false
                                    }
                                    SwipeToDismissBoxValue.Settled -> false
                                }
                            },
                            positionalThreshold = { totalDistance -> totalDistance * 0.6f }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF3B30)
                                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF6BFFB8)
                                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                                    },
                                    label = "swipeBg"
                                )
                                val label = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> "Delete"
                                    SwipeToDismissBoxValue.StartToEnd -> "Edit"
                                    SwipeToDismissBoxValue.Settled -> ""
                                }
                                val align = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    SwipeToDismissBoxValue.Settled -> Alignment.Center
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = align
                                ) {
                                    Text(label, color = OriginWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            },
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(OriginSurface, RoundedCornerShape(12.dp))
                                    .border(1.dp, OriginOutline, RoundedCornerShape(12.dp))
                                    .clickable { onSelectUrl(item.url) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
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
                                    Text(
                                        text = item.url,
                                        color = OriginMutedText,
                                        fontSize = 12.sp,
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
                    IconButton(
                        onClick = { showAddCustom = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_new_bookmark),
                            contentDescription = "New Bookmark",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                }
            }
        }

        // Edit dialog
        if (editItem != null) {
            AlertDialog(
                onDismissRequest = { editItem = null },
                containerColor = OriginSurface,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text("Edit Bookmark", fontFamily = ntype82, color = OriginWhite, fontSize = 20.sp)
                },
                text = {
                    Column {
                        Text("URL", color = OriginMutedText, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(editItem!!.url, color = OriginWhite, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Title", color = OriginMutedText) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OriginWhite,
                                unfocusedBorderColor = OriginOutline,
                                focusedTextColor = OriginWhite,
                                unfocusedTextColor = OriginWhite,
                                focusedContainerColor = OriginDarkBackground,
                                unfocusedContainerColor = OriginDarkBackground,
                                cursorColor = OriginWhite
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (editTitle.isNotBlank()) {
                            onUpdateBookmark(editItem!!.id, editTitle.trim())
                        }
                        editItem = null
                    }) {
                        Text("Save", color = OriginWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editItem = null }) {
                        Text("Cancel", color = OriginMutedText)
                    }
                }
            )
        }
    }
}
