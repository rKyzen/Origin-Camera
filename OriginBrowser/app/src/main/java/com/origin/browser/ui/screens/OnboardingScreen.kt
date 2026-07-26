package com.origin.browser.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origin.browser.R

private val OnboardingBlack = Color(0xFF000000)
private val OnboardingWhite = Color(0xFFFFFFFF)
private val OnboardingMuted = Color(0xFF888888)

@Composable
fun OnboardingScreen(
    initialStep: Int = 0,
    onComplete: (name: String) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(initialStep) }
    var nameInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val onExpanded = remember { mutableStateMapOf<String, Boolean>() }

        if (currentStep == 1) {
            // ========== STEP 2: Collapsible How to Use ==========
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "How to Use Origin Web",
                        fontFamily = FontFamily(Font(R.font.ntype82_regular)),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnboardingWhite,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "A lightweight, privacy-first browser. No tracking, no telemetry, no accounts — everything stays on your device.",
                        color = OnboardingMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OnCollapseSection(
                        icon = { Image(painter = painterResource(id = R.drawable.ic_app_logo), contentDescription = null, modifier = Modifier.size(20.dp)) },
                        title = "Home Screen",
                        sub = "Your starting point for browsing",
                        isExpanded = onExpanded["home"] ?: false,
                        onToggle = { onExpanded["home"] = !(onExpanded["home"] ?: false) }
                    ) {
                        OnIconRow(R.drawable.ic_app_logo, "Tap the Origin logo to open the menu: Bookmarks, Settings, Find in Page, Ad Blocker, Stealth Mode, Desktop Site")
                        OnIconRow(R.drawable.ic_bookmark, "★ Star icon — open your saved bookmarks")
                        OnIconRow(R.drawable.ic_tabs, "✦ Tabs icon — view and manage open tabs")
                        OnIconRow(R.drawable.ic_settings, "⚙ Settings — customise your experience with tabbed navigation")
                        OnIconRow(0, "Search bar — type a URL or search query, then press enter")
                    }

                    OnCollapseSection(
                        icon = { Image(painter = painterResource(id = R.drawable.ic_search), contentDescription = null, modifier = Modifier.size(20.dp)) },
                        title = "Browsing Screen",
                        sub = "Where you view web pages",
                        isExpanded = onExpanded["browsing"] ?: false,
                        onToggle = { onExpanded["browsing"] = !(onExpanded["browsing"] ?: false) }
                    ) {
                        OnIconRow(R.drawable.ic_app_logo, "Header logo — tap for Home, Bookmarks, Add Bookmark, Settings, Find in Page, Ad Blocker, Stealth Mode, Desktop Site")
                        OnIconRow(0, "URL bar — tap to type an address; the blue search button loads the page")
                        OnIconRow(0, "Bottom control panel: ✦ Tabs | ← Back | ↻ Reload | → Forward | ★ Bookmark")
                        OnIconRow(0, "Header hides when scrolling down — scroll up to reveal it again")
                        OnIconRow(0, "Fullscreen video supported — overlay appears automatically")
                    }

                    OnCollapseSection(
                        icon = { Text("★", color = OnboardingWhite, fontSize = 18.sp) },
                        title = "Bookmarks",
                        sub = "Save and organise your favourite pages",
                        isExpanded = onExpanded["bookmarks"] ?: false,
                        onToggle = { onExpanded["bookmarks"] = !(onExpanded["bookmarks"] ?: false) }
                    ) {
                        OnIconRow(0, "Tap ★ on Home or in the bottom control panel to open bookmarks")
                        OnIconRow(0, "\"Bookmark Current Page\" saves the page you are viewing")
                        OnIconRow(0, "\"Add Custom Bookmark\" — manually enter a URL and title")
                        OnIconRow(0, "Swipe left (←) to delete; swipe right (→) to edit the title")
                        OnIconRow(0, "Tap any bookmark to open it in the browser")
                    }

                    OnCollapseSection(
                        icon = { Image(painter = painterResource(id = R.drawable.ic_tabs), contentDescription = null, modifier = Modifier.size(20.dp)) },
                        title = "Tabs",
                        sub = "Browse multiple pages at once",
                        isExpanded = onExpanded["tabs"] ?: false,
                        onToggle = { onExpanded["tabs"] = !(onExpanded["tabs"] ?: false) }
                    ) {
                        OnIconRow(R.drawable.ic_tabs, "Tap ✦ Tabs icon on Home or in the bottom control panel to open the Tabs screen")
                        OnIconRow(0, "Tap a tab card to switch to that page")
                        OnIconRow(0, "Swipe a tab left (←) to close it")
                        OnIconRow(0, "Bottom pill: Close | Clear All Tabs | Add Tab — all in one unified pill")
                        OnIconRow(0, "Tabs are saved automatically and restored when you reopen the app")
                    }

                    OnCollapseSection(
                        icon = { Image(painter = painterResource(id = R.drawable.ic_nav_reload), contentDescription = null, modifier = Modifier.size(20.dp)) },
                        title = "History",
                        sub = "Revisit pages you have browsed",
                        isExpanded = onExpanded["history"] ?: false,
                        onToggle = { onExpanded["history"] = !(onExpanded["history"] ?: false) }
                    ) {
                        OnIconRow(0, "Go to Settings → Privacy → Browsing History to view your history")
                        OnIconRow(0, "Use the filter bar to search for specific pages")
                        OnIconRow(0, "Tap any entry to revisit that page; swipe left (←) to delete an entry")
                        OnIconRow(0, "Tap \"Clear All\" to delete your entire browsing history")
                    }

                    OnCollapseSection(
                        icon = { Image(painter = painterResource(id = R.drawable.ic_settings), contentDescription = null, modifier = Modifier.size(20.dp)) },
                        title = "Settings",
                        sub = "Customise your browsing experience",
                        isExpanded = onExpanded["settings"] ?: false,
                        onToggle = { onExpanded["settings"] = !(onExpanded["settings"] ?: false) }
                    ) {
                        OnIconRow(R.drawable.ic_settings, "Tap ⚙ on the Home screen or in the logo menu to open Settings")
                        OnIconRow(0, "Bottom pill tabs: Profile | Privacy | Extras | About — tap icons to switch")
                        OnIconRow(0, "Profile — edit your name, choose a search engine (Google, DuckDuckGo, Bing), set a custom search URL, or set as default browser")
                        OnIconRow(0, "Privacy — browsing history, clear cache and cookies, ad blocker toggle, custom ad blocker URL, stealth mode")
                        OnIconRow(0, "Extras — force dark mode on all websites")
                        OnIconRow(0, "About — app version, how to use guide, and privacy guarantee")
                    }

                    OnCollapseSection(
                        icon = { Image(painter = painterResource(id = R.drawable.ic_stealth_mode), contentDescription = null, modifier = Modifier.size(20.dp)) },
                        title = "Additional Features",
                        sub = "Stealth mode, desktop site, and more",
                        isExpanded = onExpanded["extra"] ?: false,
                        onToggle = { onExpanded["extra"] = !(onExpanded["extra"] ?: false) }
                    ) {
                        OnIconRow(R.drawable.ic_stealth_mode, "Stealth Mode — the logo changes appearance and no browsing history is saved. Toggle from the logo menu or Settings Privacy tab")
                        OnIconRow(0, "Desktop Site — toggle from the logo menu to request the desktop version of any website; the page reloads automatically")
                        OnIconRow(0, "Find in Page — open from the logo menu, type a word, and use ▲▼ to jump between matches")
                        OnIconRow(0, "Custom Search Engine URL — set in Settings Profile tab (use %s as query placeholder)")
                        OnIconRow(0, "Custom Ad Blocker URL — set in Settings Privacy tab to use your own blocklist")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Box(modifier = Modifier.padding(horizontal = 28.dp)) {
                    Button(
                        onClick = { currentStep = 2 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OnboardingWhite,
                            contentColor = OnboardingBlack
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                if (currentStep == 0) {
                    // ========== STEP 1: Feature Pitch ==========
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Origin Logo",
                        modifier = Modifier.size(100.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Welcome to Origin",
                        fontFamily = FontFamily(Font(R.font.ntype82_regular)),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnboardingWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    val features = listOf(
                        "Extremely Lightweight" to "Fast startup, zero bloat, low resource usage.",
                        "Privacy First" to "No account required, zero tracking, zero telemetry.",
                        "Distraction-Free" to "High-contrast monochrome aesthetic designed for focus."
                    )

                    features.forEach { (title, desc) ->
                        FeatureItem(title = title, description = desc)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else {
                    // ========== STEP 3: Name Input ==========
                    Spacer(modifier = Modifier.height(60.dp))

                    Text(
                        text = "What should we call you?",
                        fontFamily = FontFamily(Font(R.font.ntype82_regular)),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnboardingWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = {
                            Text(
                                text = "Enter your name",
                                color = OnboardingMuted,
                                fontSize = 15.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OnboardingWhite,
                            unfocusedBorderColor = OnboardingWhite,
                            focusedTextColor = OnboardingWhite,
                            unfocusedTextColor = OnboardingWhite,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = OnboardingWhite
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (nameInput.isNotBlank()) {
                                onComplete(nameInput.trim())
                            }
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // ========== BOTTOM BUTTON ==========
                if (currentStep == 0) {
                    Button(
                        onClick = { currentStep = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OnboardingWhite,
                            contentColor = OnboardingBlack
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            val name = nameInput.trim()
                            if (name.isNotBlank()) {
                                onComplete(name)
                            }
                        },
                        enabled = nameInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OnboardingWhite,
                            contentColor = OnboardingBlack,
                            disabledContainerColor = OnboardingWhite.copy(alpha = 0.3f),
                            disabledContentColor = OnboardingBlack.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun FeatureItem(title: String, description: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .background(OnboardingWhite, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontFamily = FontFamily(Font(R.font.ntype82_regular)),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnboardingWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = OnboardingMuted,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun OnCollapseSection(
    icon: @Composable () -> Unit,
    title: String,
    sub: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .background(OnboardingWhite.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = OnboardingWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = sub,
                    color = OnboardingMuted.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
            Text(
                text = if (isExpanded) "▲" else "▼",
                color = OnboardingMuted,
                fontSize = 11.sp
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 4.dp, end = 4.dp, bottom = 2.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun OnIconRow(iconRes: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (iconRes != 0) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp, start = 2.dp)
                    .size(4.dp)
                    .background(OnboardingMuted, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = OnboardingMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
