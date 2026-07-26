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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origin.browser.R
import com.origin.browser.ui.theme.*

@Composable
fun HowToUseScreen(
    onClose: () -> Unit
) {
    val ntype82 = FontFamily(Font(R.font.ntype82_regular))
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OriginDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "How to Use Origin Web",
                    fontFamily = ntype82,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OriginWhite
                )
                TextButton(onClick = onClose) {
                    Text("Close", color = OriginWhite, fontSize = 14.sp)
                }
            }

            Text(
                text = "A lightweight, privacy-first browser. No tracking, no telemetry, no accounts — everything stays on your device.",
                color = OriginMutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp, end = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                CollapseSection(
                    icon = { Image(painter = painterResource(id = R.drawable.ic_app_logo), contentDescription = null, modifier = Modifier.size(22.dp)) },
                    title = "Home Screen",
                    sub = "Your starting point for browsing",
                    isExpanded = expanded["home"] ?: false,
                    onToggle = { expanded["home"] = !(expanded["home"] ?: false) }
                ) {
                    HUIconRow(R.drawable.ic_app_logo, "Tap the Origin logo to open the menu: Bookmarks, Settings, Find in Page, Ad Blocker, Stealth Mode, Desktop Site")
                    HUIconRow(R.drawable.ic_bookmark, "★ Star icon — open your saved bookmarks")
                    HUIconRow(R.drawable.ic_tabs, "✦ Tabs icon — view and manage open tabs")
                    HUIconRow(R.drawable.ic_settings, "⚙ Settings — customise your experience with tabbed navigation")
                    HUIconRow(0, "Search bar — type a URL or search query, then press enter")
                }

                CollapseSection(
                    icon = { Image(painter = painterResource(id = R.drawable.ic_search), contentDescription = null, modifier = Modifier.size(22.dp)) },
                    title = "Browsing Screen",
                    sub = "Where you view web pages",
                    isExpanded = expanded["browsing"] ?: false,
                    onToggle = { expanded["browsing"] = !(expanded["browsing"] ?: false) }
                ) {
                    HUIconRow(R.drawable.ic_app_logo, "Header logo — tap for Home, Bookmarks, Add Bookmark, Settings, Find in Page, Ad Blocker, Stealth Mode, Desktop Site")
                    HUIconRow(0, "URL bar — tap to type an address and press enter or the search button")
                    HUIconRow(0, "Bottom control panel: ✦ Tabs | ← Back | ↻ Reload | → Forward | ★ Bookmark")
                    HUIconRow(0, "Header hides when scrolling down — scroll up to reveal it again")
                    HUIconRow(0, "Fullscreen video supported — overlay appears automatically")
                }

                CollapseSection(
                    icon = { Text("★", color = OriginWhite, fontSize = 20.sp) },
                    title = "Bookmarks",
                    sub = "Save and organise your favourite pages",
                    isExpanded = expanded["bookmarks"] ?: false,
                    onToggle = { expanded["bookmarks"] = !(expanded["bookmarks"] ?: false) }
                ) {
                    HUIconRow(0, "Tap ★ on Home or in the bottom control panel to open bookmarks")
                    HUIconRow(0, "\"Bookmark Current Page\" saves the page you are viewing")
                    HUIconRow(0, "\"Add Custom Bookmark\" — manually enter a URL and title")
                    HUIconRow(0, "Swipe left (←) to delete; swipe right (→) to edit the title")
                    HUIconRow(0, "Tap any bookmark to open it in the browser")
                }

                CollapseSection(
                    icon = { Image(painter = painterResource(id = R.drawable.ic_tabs), contentDescription = null, modifier = Modifier.size(22.dp)) },
                    title = "Tabs",
                    sub = "Browse multiple pages at once",
                    isExpanded = expanded["tabs"] ?: false,
                    onToggle = { expanded["tabs"] = !(expanded["tabs"] ?: false) }
                ) {
                    HUIconRow(R.drawable.ic_tabs, "Tap ✦ Tabs icon on Home or in the bottom control panel to open the Tabs screen")
                    HUIconRow(0, "Tap a tab card to switch to that page")
                    HUIconRow(0, "Swipe a tab left (←) to close it")
                    HUIconRow(0, "Bottom pill: Close | Clear All Tabs | Add Tab — all in one unified pill")
                    HUIconRow(0, "Tabs are saved automatically and restored when you reopen the app")
                }

                CollapseSection(
                    icon = { Image(painter = painterResource(id = R.drawable.ic_nav_reload), contentDescription = null, modifier = Modifier.size(22.dp)) },
                    title = "History",
                    sub = "Revisit pages you have browsed",
                    isExpanded = expanded["history"] ?: false,
                    onToggle = { expanded["history"] = !(expanded["history"] ?: false) }
                ) {
                    HUIconRow(0, "Go to Settings → Privacy → Browsing History to view your history")
                    HUIconRow(0, "Use the filter bar to search for specific pages")
                    HUIconRow(0, "Tap any entry to revisit that page; swipe left (←) to delete an entry")
                    HUIconRow(0, "Tap \"Clear All\" to delete your entire browsing history")
                }

                CollapseSection(
                    icon = { Image(painter = painterResource(id = R.drawable.ic_settings), contentDescription = null, modifier = Modifier.size(22.dp)) },
                    title = "Settings",
                    sub = "Customise your browsing experience",
                    isExpanded = expanded["settings"] ?: false,
                    onToggle = { expanded["settings"] = !(expanded["settings"] ?: false) }
                ) {
                    HUIconRow(R.drawable.ic_settings, "Tap ⚙ on the Home screen or in the logo menu to open Settings")
                    HUIconRow(0, "Bottom pill tabs: Profile | Privacy | Extras | About — tap icons to switch")
                    HUIconRow(0, "Profile — edit your name, choose a search engine (Google, DuckDuckGo, Bing), set a custom search URL, or set as default browser")
                    HUIconRow(0, "Privacy — browsing history, clear cache and cookies, ad blocker toggle, custom ad blocker URL, stealth mode")
                    HUIconRow(0, "Extras — force dark mode on all websites")
                    HUIconRow(0, "About — app version, how to use guide, and privacy guarantee")
                }

                CollapseSection(
                    icon = { Image(painter = painterResource(id = R.drawable.ic_stealth_mode), contentDescription = null, modifier = Modifier.size(22.dp)) },
                    title = "Additional Features",
                    sub = "Stealth mode, desktop site, and more",
                    isExpanded = expanded["extra"] ?: false,
                    onToggle = { expanded["extra"] = !(expanded["extra"] ?: false) }
                ) {
                    HUIconRow(R.drawable.ic_stealth_mode, "Stealth Mode — the logo changes appearance and no browsing history is saved. Toggle from the logo menu or Settings Privacy tab")
                    HUIconRow(0, "Desktop Site — toggle from the logo menu to request the desktop version of any website; the page reloads automatically")
                    HUIconRow(0, "Find in Page — open from the logo menu, type a word, and use ▲▼ to jump between matches")
                    HUIconRow(0, "Custom Search Engine URL — set in Settings Profile tab (use %s as query placeholder)")
                    HUIconRow(0, "Custom Ad Blocker URL — set in Settings Privacy tab to use your own blocklist")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CollapseSection(
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
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .background(OriginSurfaceVariant, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = OriginWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = sub,
                    color = OriginMutedText.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
            Text(
                text = if (isExpanded) "▲" else "▼",
                color = OriginMutedText,
                fontSize = 12.sp
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
                    .padding(start = 8.dp, top = 6.dp, end = 4.dp, bottom = 4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun HUIconRow(iconRes: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (iconRes != 0) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, start = 4.dp)
                    .size(5.dp)
                    .background(OriginMutedText, RoundedCornerShape(2.5.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            color = OriginMutedText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
