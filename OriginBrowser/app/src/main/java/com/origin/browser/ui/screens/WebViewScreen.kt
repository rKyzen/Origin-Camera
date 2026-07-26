package com.origin.browser.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.*
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.origin.browser.R
import com.origin.browser.data.adblock.AdBlocker
import com.origin.browser.ui.theme.*
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    urlToLoad: String,
    isAdBlockEnabled: Boolean,
    isStealthMode: Boolean,
    isBookmarked: Boolean,
    tabsCount: Int = 0,
    onSearch: (String) -> Unit,
    onPageLoaded: (title: String, url: String) -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onToggleStealthMode: () -> Unit,
    onGoHome: () -> Unit
) {
    var currentUrl by remember { mutableStateOf(urlToLoad) }
    var pageTitle by remember { mutableStateOf("") }
    var inputUrlText by remember { mutableStateOf(urlToLoad) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }

    // Desktop Site state
    var isDesktopSite by remember { mutableStateOf(false) }

    // Fullscreen video state
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Find in Page state
    var isFindInPageOpen by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findMatchOrdinal by remember { mutableIntStateOf(0) }
    var findMatchCount by remember { mutableIntStateOf(0) }

    // Page loading progress state
    var loadProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    // Scroll-aware header hiding
    var isHeaderVisible by remember { mutableStateOf(true) }
    var scrollAccumulator by remember { mutableIntStateOf(0) }
    var wasAtTop by remember { mutableStateOf(true) }

    // Desktop mode state
    var isDesktopMode by remember { mutableStateOf(false) }

    LaunchedEffect(isDesktopMode) {
        webViewInstance?.settings?.userAgentString = if (isDesktopMode) {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else {
            null
        }
    }

    // Synchronize input text with URL changes
    LaunchedEffect(urlToLoad) {
        currentUrl = urlToLoad
        inputUrlText = urlToLoad
        if (urlToLoad.isNotBlank()) {
            webViewInstance?.loadUrl(urlToLoad)
        } else {
            webViewInstance?.stopLoading()
            webViewInstance?.onPause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OriginDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

            // ==================== COLLAPSIBLE HEADER ====================
            AnimatedVisibility(
                visible = isHeaderVisible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                Column {
                    // ==================== TOP HEADER BAR ====================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OriginDarkBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Interactive APP ui logo dropdown anchor
                        Box {
                            IconButton(
                                onClick = { isMenuOpen = !isMenuOpen },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = if (isStealthMode) R.drawable.ic_stealth_mode else R.drawable.ic_app_logo),
                                    contentDescription = "Logo Menu",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Logo Dropdown Menu
                            DropdownMenu(
                                expanded = isMenuOpen,
                                onDismissRequest = { isMenuOpen = false },
                                modifier = Modifier
                                    .background(OriginDarkBackground.copy(alpha = 0.75f))
                                    .border(1.dp, OriginOutline, RoundedCornerShape(8.dp))
                            ) {
                                Surface(
                                    color = Color.Transparent,
                                    tonalElevation = 0.dp
                                ) {
                                    Column {
                                        DropdownMenuItem(
                                            text = { Text("Home", color = OriginWhite) },
                                            leadingIcon = { Image(painter = painterResource(id = R.drawable.ic_home), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                isMenuOpen = false
                                                onGoHome()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Bookmarks", color = OriginWhite) },
                                            leadingIcon = { Image(painter = painterResource(id = R.drawable.ic_bookmark), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                isMenuOpen = false
                                                onOpenBookmarks()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (isBookmarked) "Remove Bookmark" else "Add Bookmark",
                                                    color = OriginWhite
                                                )
                                            },
                                            leadingIcon = { Image(painter = painterResource(id = R.drawable.ic_bookmerked), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                isMenuOpen = false
                                                onToggleBookmark()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Settings", color = OriginWhite) },
                                            leadingIcon = { Image(painter = painterResource(id = R.drawable.ic_settings), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                isMenuOpen = false
                                                onOpenSettings()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Find in Page", color = OriginWhite) },
                                            leadingIcon = { Image(painter = painterResource(id = R.drawable.ic_find_in_page), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                isMenuOpen = false
                                                isFindInPageOpen = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (isAdBlockEnabled) "Ad Blocker: ON" else "Ad Blocker: OFF",
                                                    color = if (isAdBlockEnabled) Color(0xFF6BFFB8) else OriginWhite
                                                )
                                            },
                                            leadingIcon = { Image(painter = painterResource(id = R.drawable.ic_ad_blocker), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                isMenuOpen = false
                                                onToggleAdBlock()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (isStealthMode) "Stealth Mode: ON" else "Stealth Mode: OFF",
                                                    color = if (isStealthMode) Color(0xFF6BFFB8) else OriginWhite
                                                )
                                            },
                                            leadingIcon = { Image(painter = painterResource(id = R.drawable.ic_stealth_mode), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                isMenuOpen = false
                                                onToggleStealthMode()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (isDesktopSite) "Desktop Site: ON" else "Desktop Site: OFF",
                                                    color = if (isDesktopSite) Color(0xFF6BFFB8) else OriginWhite
                                                )
                                            },
                                            leadingIcon = { Image(painter = painterResource(id = R.drawable.ic_desktop_site), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                isDesktopSite = !isDesktopSite
                                                webViewInstance?.settings?.userAgentString = if (isDesktopSite) {
                                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                                } else {
                                                    null
                                                }
                                                webViewInstance?.reload()
                                                isMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Compact Pill Search Bar
                        OutlinedTextField(
                            value = inputUrlText,
                            onValueChange = { inputUrlText = it },
                            placeholder = {
                                Text(
                                    text = "Type here or search a URL",
                                    color = OriginMutedText,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OriginWhite,
                                unfocusedBorderColor = OriginWhite,
                                focusedTextColor = OriginWhite,
                                unfocusedTextColor = OriginWhite,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = OriginWhite
                            ),
                            textStyle = TextStyle(fontSize = 14.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (inputUrlText.isNotBlank()) {
                                    onSearch(inputUrlText.trim())
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Far right search button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.dp, OriginWhite, CircleShape)
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (inputUrlText.isNotBlank()) {
                                        onSearch(inputUrlText.trim())
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_search),
                                    contentDescription = "Search",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // ==================== LOADING PROGRESS BAR ====================
                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { loadProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp),
                            color = OriginWhite,
                            trackColor = Color.Transparent
                        )
                    }

                    // ==================== FIND IN PAGE BAR ====================
                    if (isFindInPageOpen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(OriginSurface)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                    OutlinedTextField(
                        value = findQuery,
                        onValueChange = { query ->
                            findQuery = query
                            webViewInstance?.findAllAsync(query)
                        },
                        placeholder = { Text("Find in page...", color = OriginMutedText, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OriginWhite,
                            unfocusedBorderColor = OriginOutline,
                            focusedTextColor = OriginWhite,
                            unfocusedTextColor = OriginWhite,
                            focusedContainerColor = OriginDarkBackground,
                            unfocusedContainerColor = OriginDarkBackground
                        ),
                        textStyle = TextStyle(fontSize = 13.sp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (findQuery.isNotEmpty()) {
                        Text(
                            text = if (findMatchCount > 0) "${findMatchOrdinal + 1}/$findMatchCount" else "0/0",
                            color = OriginMutedText,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.findNext(false) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("▲", color = OriginWhite, fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = { webViewInstance?.findNext(true) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("▼", color = OriginWhite, fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = {
                            isFindInPageOpen = false
                            findQuery = ""
                            webViewInstance?.clearMatches()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("✕", color = OriginMutedText, fontSize = 14.sp)
                    }
                    }
                }
                }
            }

            // ==================== MAIN WEBVIEW VIEWPORT ====================
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // General settings
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        @Suppress("DEPRECATION")
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        // Credential Management (Autofill Framework)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                        }

                        // Cookie Persistence
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        // Force Dark Mode setup
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
                        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                            @Suppress("DEPRECATION")
                            WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
                        }

                        // Find in page listener
                        setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                            findMatchOrdinal = activeMatchOrdinal
                            findMatchCount = numberOfMatches
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                loadProgress = newProgress
                                isLoading = newProgress < 100
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
                                }
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                super.onShowCustomView(view, callback)
                                customView = view
                                customViewCallback = callback
                            }

                            override fun onHideCustomView() {
                                super.onHideCustomView()
                                customViewCallback?.onCustomViewHidden()
                                customViewCallback = null
                                customView = null
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                url?.let {
                                    currentUrl = it
                                    inputUrlText = it
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.let { wv ->
                                    canGoBack = wv.canGoBack()
                                    canGoForward = wv.canGoForward()

                                    val finalUrl = url ?: wv.url ?: ""
                                    val finalTitle = wv.title ?: pageTitle
                                    if (finalUrl.isNotBlank()) {
                                        onPageLoaded(finalTitle, finalUrl)
                                    }

                                    // Cookie flush
                                    CookieManager.getInstance().flush()
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                if (isAdBlockEnabled && request != null) {
                                    val reqUrl = request.url.toString()
                                    if (AdBlocker.shouldBlock(reqUrl)) {
                                        return WebResourceResponse(
                                            "text/plain",
                                            "utf-8",
                                            ByteArrayInputStream("".toByteArray())
                                        )
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                            val delta = scrollY - oldScrollY
                            if (delta > 0) {
                                scrollAccumulator = (scrollAccumulator + delta).coerceAtMost(60)
                            } else if (delta < 0) {
                                scrollAccumulator = (scrollAccumulator + delta).coerceAtLeast(0)
                            }
                            if (isHeaderVisible && scrollAccumulator >= 40 && scrollY > 0) {
                                isHeaderVisible = false
                            } else if (!isHeaderVisible && (scrollAccumulator <= 5 || scrollY == 0)) {
                                isHeaderVisible = true
                            }
                            wasAtTop = scrollY == 0
                        }

                        loadUrl(urlToLoad)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                    if (webView.url != urlToLoad && urlToLoad.isNotBlank()) {
                        webView.loadUrl(urlToLoad)
                    }
                },
                modifier = Modifier.weight(1f)
            )
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
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .border(1.dp, OriginWhite, RoundedCornerShape(28.dp))
                        .wrapContentSize()
                ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onOpenTabs() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tabs),
                            contentDescription = "Tabs",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nav_back),
                            contentDescription = "Back",
                            alpha = if (canGoBack) 1.0f else 0.35f,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nav_reload),
                            contentDescription = "Reload",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nav_forward),
                            contentDescription = "Forward",
                            alpha = if (canGoForward) 1.0f else 0.35f,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onToggleBookmark() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(
                            text = if (isBookmarked) "★" else "☆",
                            color = OriginWhite,
                            fontSize = 20.sp
                        )
                    }
                }
                }
            }
        }

        // Fullscreen video overlay
        if (customView != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.FrameLayout(ctx).also { fl ->
                            fl.addView(customView)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
