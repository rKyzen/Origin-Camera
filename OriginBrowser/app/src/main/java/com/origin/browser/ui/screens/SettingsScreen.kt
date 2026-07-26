package com.origin.browser.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origin.browser.R
import com.origin.browser.ui.theme.*

enum class SearchEngine(val label: String, val searchUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    BING("Bing", "https://www.bing.com/search?q=")
}

private enum class SettingsTab(val label: String, val iconRes: Int) {
    PROFILE("Profile", R.drawable.ic_personalisation),
    PRIVACY("Privacy", R.drawable.ic_privacy),
    EXTRAS("Extras", R.drawable.ic_extras),
    ABOUT("About", R.drawable.ic_about)
}

@Composable
fun SettingsScreen(
    userName: String,
    defaultSearchEngine: SearchEngine,
    isAdBlockEnabled: Boolean,
    isForceDarkMode: Boolean,
    isStealthMode: Boolean,
    customSearchEngineUrl: String,
    customAdBlockerUrl: String,
    onClose: () -> Unit,
    onSaveName: (String) -> Unit,
    onSearchEngineChanged: (SearchEngine) -> Unit,
    onToggleAdBlock: () -> Unit,
    onToggleForceDarkMode: () -> Unit,
    onToggleStealthMode: () -> Unit,
    onCustomSearchEngineUrlChanged: (String) -> Unit,
    onCustomAdBlockerUrlChanged: (String) -> Unit,
    onClearBrowsingData: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenHowToUse: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(SettingsTab.PROFILE) }

    val ntype82 = FontFamily(androidx.compose.ui.text.font.Font(R.font.ntype82_regular))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OriginDarkBackground.copy(alpha = 0.95f))
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Settings",
                    fontFamily = ntype82,
                    color = OriginWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClose) {
                    Text("Close", color = OriginWhite, fontSize = 14.sp)
                }
            }

            // Tab content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    SettingsTab.PROFILE -> ProfileTab(
                        userName = userName,
                        defaultSearchEngine = defaultSearchEngine,
                        customSearchEngineUrl = customSearchEngineUrl,
                        onSaveName = onSaveName,
                        onSearchEngineChanged = onSearchEngineChanged,
                        onCustomSearchEngineUrlChanged = onCustomSearchEngineUrlChanged,
                        ntype82 = ntype82
                    )
                    SettingsTab.PRIVACY -> PrivacyTab(
                        isAdBlockEnabled = isAdBlockEnabled,
                        isStealthMode = isStealthMode,
                        customAdBlockerUrl = customAdBlockerUrl,
                        onToggleAdBlock = onToggleAdBlock,
                        onToggleStealthMode = onToggleStealthMode,
                        onCustomAdBlockerUrlChanged = onCustomAdBlockerUrlChanged,
                        onClearBrowsingData = onClearBrowsingData,
                        onOpenHistory = onOpenHistory
                    )
                    SettingsTab.EXTRAS -> ExtrasTab(
                        isForceDarkMode = isForceDarkMode,
                        onToggleForceDarkMode = onToggleForceDarkMode
                    )
                    SettingsTab.ABOUT -> AboutTab(onOpenHowToUse = onOpenHowToUse)
                }
            }
        }

        // Bottom pill tab selector
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Close",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    SettingsTab.entries.forEach { tab ->
                        val isActive = selectedTab == tab
                        Surface(
                            color = if (isActive) OriginWhite else Color.Transparent,
                            shape = CircleShape,
                            modifier = Modifier
                                .clickable { selectedTab = tab }
                                .size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = tab.iconRes),
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp),
                                    colorFilter = if (isActive) androidx.compose.ui.graphics.ColorFilter.tint(OriginDarkBackground) else null
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ProfileTab(
    userName: String,
    defaultSearchEngine: SearchEngine,
    customSearchEngineUrl: String,
    onSaveName: (String) -> Unit,
    onSearchEngineChanged: (SearchEngine) -> Unit,
    onCustomSearchEngineUrlChanged: (String) -> Unit,
    ntype82: FontFamily
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var editingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(userName) }
    var showEngineMenu by remember { mutableStateOf(false) }
    var editingCustomSearchUrl by remember { mutableStateOf(false) }
    var customSearchUrlInput by remember { mutableStateOf(customSearchEngineUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("Profile & Personalisation", ntype82)

        // Greeting Name
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!editingName) editingName = true }
                    .padding(16.dp)
            ) {
                Text("User Greeting Name", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                if (editingName) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OriginWhite,
                            unfocusedBorderColor = OriginOutline,
                            focusedTextColor = OriginWhite,
                            unfocusedTextColor = OriginWhite,
                            focusedContainerColor = OriginSurface,
                            unfocusedContainerColor = OriginSurface,
                            cursorColor = OriginWhite
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            editingName = false
                            onSaveName(nameInput)
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = {
                            editingName = false
                            nameInput = userName
                        }) {
                            Text("Cancel", color = OriginMutedText, fontSize = 13.sp)
                        }
                        TextButton(onClick = {
                            editingName = false
                            onSaveName(nameInput)
                        }) {
                            Text("Save", color = OriginWhite, fontSize = 13.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (userName.isNotBlank()) userName else "Tap to set name",
                            color = OriginMutedText,
                            fontSize = 14.sp
                        )
                        Text("Edit", color = OriginOutlineHighlight, fontSize = 13.sp)
                    }
                }
            }
        }

        // Default Search Engine
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEngineMenu = true }
                    .padding(16.dp)
            ) {
                Text("Default Search Engine", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(defaultSearchEngine.label, color = OriginMutedText, fontSize = 14.sp)
                    Text("▾", color = OriginOutlineHighlight, fontSize = 16.sp)
                }
            }
            DropdownMenu(
                expanded = showEngineMenu,
                onDismissRequest = { showEngineMenu = false },
                modifier = Modifier
                    .background(OriginSurface)
                    .border(1.dp, OriginOutline, RoundedCornerShape(8.dp))
            ) {
                SearchEngine.entries.forEach { engine ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                engine.label,
                                color = if (engine == defaultSearchEngine) OriginWhite else OriginMutedText
                            )
                        },
                        onClick = {
                            showEngineMenu = false
                            onSearchEngineChanged(engine)
                        }
                    )
                }
            }
        }

        // Custom Search Engine URL
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!editingCustomSearchUrl) editingCustomSearchUrl = true }
                    .padding(16.dp)
            ) {
                Text("Custom Search Engine URL", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Overrides default engine. Use %s as the query placeholder",
                    color = OriginMutedText,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (editingCustomSearchUrl) {
                    OutlinedTextField(
                        value = customSearchUrlInput,
                        onValueChange = { customSearchUrlInput = it },
                        placeholder = { Text("https://...?q=%s", color = OriginMutedText, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OriginWhite,
                            unfocusedBorderColor = OriginOutline,
                            focusedTextColor = OriginWhite,
                            unfocusedTextColor = OriginWhite,
                            focusedContainerColor = OriginSurface,
                            unfocusedContainerColor = OriginSurface,
                            cursorColor = OriginWhite
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            editingCustomSearchUrl = false
                            onCustomSearchEngineUrlChanged(customSearchUrlInput.trim())
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = {
                            editingCustomSearchUrl = false
                            customSearchUrlInput = customSearchEngineUrl
                        }) {
                            Text("Cancel", color = OriginMutedText, fontSize = 13.sp)
                        }
                        TextButton(onClick = {
                            editingCustomSearchUrl = false
                            onCustomSearchEngineUrlChanged(customSearchUrlInput.trim())
                        }) {
                            Text("Save", color = OriginWhite, fontSize = 13.sp)
                        }
                    }
                } else {
                    Text(
                        text = if (customSearchEngineUrl.isNotBlank()) customSearchEngineUrl else "Tap to set custom search URL",
                        color = OriginMutedText,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Set as Default Browser
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                        context.startActivity(intent)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Set as Default Browser", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Open system settings to make Origin your default", color = OriginMutedText, fontSize = 13.sp)
                }
                Text("Open", color = OriginOutlineHighlight, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun PrivacyTab(
    isAdBlockEnabled: Boolean,
    isStealthMode: Boolean,
    customAdBlockerUrl: String,
    onToggleAdBlock: () -> Unit,
    onToggleStealthMode: () -> Unit,
    onCustomAdBlockerUrlChanged: (String) -> Unit,
    onClearBrowsingData: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var editingCustomAdBlockerUrl by remember { mutableStateOf(false) }
    var customAdBlockerUrlInput by remember { mutableStateOf(customAdBlockerUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("Privacy", FontFamily(androidx.compose.ui.text.font.Font(R.font.ntype82_regular)))

        // Browsing History
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenHistory() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Browsing History", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("View, search, or delete visited sites", color = OriginMutedText, fontSize = 13.sp)
                }
                Text("View →", color = OriginOutlineHighlight, fontSize = 13.sp)
            }
        }

        // Clear Browsing Data
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClearBrowsingData() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Clear Browsing Data", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Clear cache, cookies, and site storage", color = OriginMutedText, fontSize = 13.sp)
                }
                Text("Clear", color = Color(0xFFFF6B6B), fontSize = 13.sp)
            }
        }

        // Block Ads & Trackers
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Block Ads & Trackers", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isAdBlockEnabled) "Ad blocking is active" else "Allow ads and trackers",
                        color = OriginMutedText,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = isAdBlockEnabled,
                    onCheckedChange = { onToggleAdBlock() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = OriginWhite,
                        uncheckedTrackColor = OriginOutline,
                        checkedThumbColor = OriginDarkBackground,
                        uncheckedThumbColor = OriginMutedText
                    )
                )
            }
        }

        // Custom Ad Blocker URL
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!editingCustomAdBlockerUrl) editingCustomAdBlockerUrl = true }
                    .padding(16.dp)
            ) {
                Text("Custom Ad Blocker URL", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Override the default blocklist URL used for ad filtering",
                    color = OriginMutedText,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (editingCustomAdBlockerUrl) {
                    OutlinedTextField(
                        value = customAdBlockerUrlInput,
                        onValueChange = { customAdBlockerUrlInput = it },
                        placeholder = { Text("https://...", color = OriginMutedText, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OriginWhite,
                            unfocusedBorderColor = OriginOutline,
                            focusedTextColor = OriginWhite,
                            unfocusedTextColor = OriginWhite,
                            focusedContainerColor = OriginSurface,
                            unfocusedContainerColor = OriginSurface,
                            cursorColor = OriginWhite
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            editingCustomAdBlockerUrl = false
                            onCustomAdBlockerUrlChanged(customAdBlockerUrlInput.trim())
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = {
                            editingCustomAdBlockerUrl = false
                            customAdBlockerUrlInput = customAdBlockerUrl
                        }) {
                            Text("Cancel", color = OriginMutedText, fontSize = 13.sp)
                        }
                        TextButton(onClick = {
                            editingCustomAdBlockerUrl = false
                            onCustomAdBlockerUrlChanged(customAdBlockerUrlInput.trim())
                        }) {
                            Text("Save", color = OriginWhite, fontSize = 13.sp)
                        }
                    }
                } else {
                    Text(
                        text = if (customAdBlockerUrl.isNotBlank()) customAdBlockerUrl else "Tap to set custom ad blocker URL",
                        color = OriginMutedText,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Stealth Mode
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stealth Mode", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isStealthMode) "No history or data saved — logo changes to stealth icon" else "Browsing data will be saved",
                        color = OriginMutedText,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = isStealthMode,
                    onCheckedChange = { onToggleStealthMode() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = OriginWhite,
                        uncheckedTrackColor = OriginOutline,
                        checkedThumbColor = OriginDarkBackground,
                        uncheckedThumbColor = OriginMutedText
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun ExtrasTab(
    isForceDarkMode: Boolean,
    onToggleForceDarkMode: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("Extras", FontFamily(androidx.compose.ui.text.font.Font(R.font.ntype82_regular)))

        // Force Dark Mode
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Force Webpage Dark Mode", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isForceDarkMode) "Dark theme forced on all sites" else "Sites use their own theme",
                        color = OriginMutedText,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = isForceDarkMode,
                    onCheckedChange = { onToggleForceDarkMode() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = OriginWhite,
                        uncheckedTrackColor = OriginOutline,
                        checkedThumbColor = OriginDarkBackground,
                        uncheckedThumbColor = OriginMutedText
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun AboutTab(onOpenHowToUse: () -> Unit) {
    val ntype82 = FontFamily(androidx.compose.ui.text.font.Font(R.font.ntype82_regular))
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("About & Zero-Data Guarantee", ntype82)

        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("App Version", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("v1.0.0", color = OriginMutedText, fontSize = 14.sp)
                }
                HorizontalDivider(color = OriginOutline, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenHowToUse() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("How to Use", color = OriginWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("→", color = OriginMutedText, fontSize = 14.sp)
                }
                Text(
                    text = "Origin Browser collects zero telemetry, requires no accounts, and stores all data strictly on your device.",
                    color = OriginMutedText,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SectionHeader(title: String, headingFont: FontFamily) {
    Text(
        text = title,
        fontFamily = headingFont,
        color = OriginMutedText.copy(alpha = 0.7f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OriginSurface, RoundedCornerShape(14.dp))
            .border(1.dp, OriginOutline, RoundedCornerShape(14.dp)),
        content = content
    )
}
