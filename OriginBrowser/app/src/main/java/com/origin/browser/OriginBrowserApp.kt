package com.origin.browser

import android.content.Context
import android.webkit.CookieManager
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.origin.browser.data.db.OriginDatabase
import com.origin.browser.data.db.entity.BookmarkEntry
import com.origin.browser.data.db.entity.HistoryEntry
import com.origin.browser.data.model.TabData
import com.origin.browser.ui.screens.BookmarksScreen
import com.origin.browser.ui.screens.HistoryScreen
import com.origin.browser.ui.screens.HomeScreen
import com.origin.browser.ui.screens.HowToUseScreen
import com.origin.browser.ui.screens.NameInputDialog
import com.origin.browser.ui.screens.OnboardingScreen
import com.origin.browser.ui.screens.SearchEngine
import com.origin.browser.ui.screens.SettingsScreen
import com.origin.browser.ui.screens.TabsScreen
import com.origin.browser.ui.screens.WebViewScreen
import kotlinx.coroutines.launch

@Composable
fun OriginBrowserApp(initialUrl: String = "") {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Room DB instance
    val db = remember { OriginDatabase.getDatabase(context) }
    val historyDao = remember { db.historyDao() }
    val bookmarkDao = remember { db.bookmarkDao() }

    // SharedPreferences for settings & user info
    val prefs = remember { context.getSharedPreferences("origin_prefs", Context.MODE_PRIVATE) }
    var isAdBlockEnabled by remember {
        mutableStateOf(prefs.getBoolean("ad_block_enabled", true))
    }
    var userName by remember {
        mutableStateOf(prefs.getString("user_name", "") ?: "")
    }
    var isFirstLaunch by remember {
        mutableStateOf(prefs.getBoolean("is_first_launch", true))
    }

    // Onboarding dialog state
    var showNameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userName) {
        if (userName.isBlank() && !isFirstLaunch) {
            showNameDialog = true
        }
    }

    // Settings state
    var defaultSearchEngine by remember {
        mutableStateOf(
            try { SearchEngine.valueOf(prefs.getString("search_engine", "GOOGLE") ?: "GOOGLE") }
            catch (e: Exception) { SearchEngine.GOOGLE }
        )
    }
    var isForceDarkMode by remember {
        mutableStateOf(prefs.getBoolean("force_dark_mode", false))
    }
    var isStealthMode by remember {
        mutableStateOf(prefs.getBoolean("stealth_mode", false))
    }
    var customSearchEngineUrl by remember {
        mutableStateOf(prefs.getString("custom_search_engine_url", "") ?: "")
    }
    var customAdBlockerUrl by remember {
        mutableStateOf(prefs.getString("custom_ad_blocker_url", "") ?: "")
    }

    // Navigation and state
    var currentUrl by remember { mutableStateOf("") }
    var currentTitle by remember { mutableStateOf("") }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showBookmarksScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showTabsScreen by remember { mutableStateOf(false) }
    var showHowToUse by remember { mutableStateOf(false) }

    // Persisted tab list
    fun loadTabs(): List<TabData> {
        val count = prefs.getInt("tab_count", 0)
        val list = mutableListOf<TabData>()
        for (i in 0 until count) {
            val id = prefs.getLong("tab_id_$i", 0)
            val url = prefs.getString("tab_url_$i", "") ?: ""
            val title = prefs.getString("tab_title_$i", "") ?: ""
            if (url.isNotBlank()) {
                list.add(TabData(id = id, url = url, title = title))
            }
        }
        return list
    }
    fun saveTabs(tabs: List<TabData>) {
        prefs.edit().putInt("tab_count", tabs.size).apply()
        tabs.forEachIndexed { index, tab ->
            prefs.edit()
                .putLong("tab_id_$index", tab.id)
                .putString("tab_url_$index", tab.url)
                .putString("tab_title_$index", tab.title)
                .apply()
        }
    }
    var tabs by remember { mutableStateOf(loadTabs()) }
    var nextTabId by remember { mutableIntStateOf(
        (0 until prefs.getInt("tab_count", 0)).maxOfOrNull {
            prefs.getLong("tab_id_$it", 0)
        }?.plus(1)?.toInt() ?: 1
    ) }
    // Save tabs whenever they change
    LaunchedEffect(tabs) {
        saveTabs(tabs)
    }

    // Handle incoming URL from external intent
    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank()) {
            currentUrl = initialUrl
        }
    }

    // Observe DB
    val historyList by historyDao.getAllHistory().collectAsState(initial = emptyList())
    val bookmarkList by bookmarkDao.getAllBookmarks().collectAsState(initial = emptyList())
    val isBookmarked by bookmarkDao.isBookmarked(currentUrl).collectAsState(initial = false)

    // Helper: Parse search input or URL
    fun processSearchInput(input: String) {
        val trimmed = input.trim()
        val formattedUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            customSearchEngineUrl.isNotBlank() -> customSearchEngineUrl.replace("%s", trimmed.replace(" ", "+"))
            else -> "${defaultSearchEngine.searchUrl}${trimmed.replace(" ", "+")}"
        }
        currentUrl = formattedUrl
        val newTab = TabData(id = nextTabId.toLong(), url = formattedUrl, title = formattedUrl)
        tabs = tabs + newTab
        nextTabId++
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentUrl.isEmpty(),
            transitionSpec = {
                if (targetState) {
                    (slideInVertically(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                        initialOffsetY = { -it }
                    ) + fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)))
                        .togetherWith(
                            slideOutVertically(
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                                targetOffsetY = { it }
                            ) + fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
                        )
                        .using(SizeTransform(clip = false))
                } else {
                    (slideInVertically(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                        initialOffsetY = { it }
                    ) + fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)))
                        .togetherWith(
                            slideOutVertically(
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                                targetOffsetY = { -it }
                            ) + fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
                        )
                        .using(SizeTransform(clip = false))
                }
            },
            label = "homeWebViewTransition"
        ) { isHome ->
            if (isHome) {
                HomeScreen(
                    userName = userName,
                    isAdBlockEnabled = isAdBlockEnabled,
                    isStealthMode = isStealthMode,
                    tabsCount = tabs.size,
                    onSearch = { input -> processSearchInput(input) },
                    onChangeName = { showNameDialog = true },
                    onOpenHistory = { showHistoryScreen = true },
                    onOpenBookmarks = { showBookmarksScreen = true },
                    onOpenTabs = { showTabsScreen = true },
                    onOpenSettings = { showSettingsScreen = true },
                    onToggleAdBlock = {
                        val newState = !isAdBlockEnabled
                        isAdBlockEnabled = newState
                        prefs.edit().putBoolean("ad_block_enabled", newState).apply()
                    },
                    onToggleStealthMode = {
                        val newState = !isStealthMode
                        isStealthMode = newState
                        prefs.edit().putBoolean("stealth_mode", newState).apply()
                    }
                )
            } else {
                WebViewScreen(
                    urlToLoad = currentUrl,
                    isAdBlockEnabled = isAdBlockEnabled,
                    isStealthMode = isStealthMode,
                    isBookmarked = isBookmarked,
                    tabsCount = tabs.size,
                    onSearch = { input -> processSearchInput(input) },
                    onPageLoaded = { title, url ->
                        if (currentUrl.isNotEmpty()) {
                            currentTitle = title
                            currentUrl = url
                            // Update the last tab's title
                            if (tabs.isNotEmpty()) {
                                val lastTab = tabs.last()
                                tabs = tabs.dropLast(1) + lastTab.copy(title = title.ifBlank { url })
                            }
                            coroutineScope.launch {
                                historyDao.insert(HistoryEntry(url = url, title = title))
                            }
                        }
                    },
                    onToggleBookmark = {
                        coroutineScope.launch {
                            if (isBookmarked) {
                                bookmarkDao.deleteByUrl(currentUrl)
                            } else {
                                bookmarkDao.insert(BookmarkEntry(url = currentUrl, title = currentTitle.ifBlank { currentUrl }))
                            }
                        }
                    },
                    onOpenHistory = { showHistoryScreen = true },
                    onOpenBookmarks = { showBookmarksScreen = true },
                    onOpenTabs = { showTabsScreen = true },
                    onOpenSettings = { showSettingsScreen = true },
                    onToggleAdBlock = {
                        val newState = !isAdBlockEnabled
                        isAdBlockEnabled = newState
                        prefs.edit().putBoolean("ad_block_enabled", newState).apply()
                    },
                    onToggleStealthMode = {
                        val newState = !isStealthMode
                        isStealthMode = newState
                        prefs.edit().putBoolean("stealth_mode", newState).apply()
                    },
                    onGoHome = { currentUrl = "" }
                )
            }
        }

        // Onboarding overlay (takes precedence over everything)
        if (isFirstLaunch) {
            OnboardingScreen(
                onComplete = { name ->
                    val trimmed = name.trim()
                    userName = trimmed
                    prefs.edit()
                        .putBoolean("is_first_launch", false)
                        .putString("user_name", trimmed)
                        .apply()
                    isFirstLaunch = false
                }
            )
        }

        // Overlays with fade animation
        if (showNameDialog) {
            NameInputDialog(
                initialName = userName,
                onSaveName = { newName ->
                    val trimmed = newName.trim()
                    if (trimmed.isNotBlank()) {
                        userName = trimmed
                        prefs.edit().putString("user_name", trimmed).apply()
                        showNameDialog = false
                    }
                },
                onDismiss = {
                    if (userName.isNotBlank()) {
                        showNameDialog = false
                    } else {
                        showNameDialog = true
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = showHistoryScreen,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)),
            exit = fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
        ) {
            HistoryScreen(
                historyList = historyList,
                onSelectUrl = { url ->
                    currentUrl = url
                    showHistoryScreen = false
                },
                onClearAll = {
                    coroutineScope.launch { historyDao.clearAll() }
                },
                onDeleteEntry = { entry ->
                    coroutineScope.launch { historyDao.deleteById(entry.id) }
                },
                onClose = { showHistoryScreen = false }
            )
        }

        AnimatedVisibility(
            visible = showBookmarksScreen,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)),
            exit = fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
        ) {
            BookmarksScreen(
                bookmarkList = bookmarkList,
                currentUrl = currentUrl,
                currentTitle = currentTitle,
                isCurrentPageBookmarked = isBookmarked,
                onAddBookmark = { url, title ->
                    coroutineScope.launch {
                        bookmarkDao.insert(BookmarkEntry(url = url, title = title))
                    }
                },
                onRemoveBookmark = { url ->
                    coroutineScope.launch {
                        bookmarkDao.deleteByUrl(url)
                    }
                },
                onDeleteBookmark = { id ->
                    coroutineScope.launch {
                        bookmarkDao.deleteById(id)
                    }
                },
                onUpdateBookmark = { id, title ->
                    coroutineScope.launch {
                        bookmarkDao.updateTitle(id, title)
                    }
                },
                onSelectUrl = { url ->
                    currentUrl = url
                    showBookmarksScreen = false
                },
                onClose = { showBookmarksScreen = false }
            )
        }

        AnimatedVisibility(
            visible = showSettingsScreen,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)),
            exit = fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
        ) {
            SettingsScreen(
                userName = userName,
                defaultSearchEngine = defaultSearchEngine,
                isAdBlockEnabled = isAdBlockEnabled,
                isForceDarkMode = isForceDarkMode,
                isStealthMode = isStealthMode,
                customSearchEngineUrl = customSearchEngineUrl,
                customAdBlockerUrl = customAdBlockerUrl,
                onClose = { showSettingsScreen = false },
                onSaveName = { name ->
                    val trimmed = name.trim()
                    if (trimmed.isNotBlank()) {
                        userName = trimmed
                        prefs.edit().putString("user_name", trimmed).apply()
                    }
                },
                onSearchEngineChanged = { engine ->
                    defaultSearchEngine = engine
                    prefs.edit().putString("search_engine", engine.name).apply()
                },
                onToggleAdBlock = {
                    val newState = !isAdBlockEnabled
                    isAdBlockEnabled = newState
                    prefs.edit().putBoolean("ad_block_enabled", newState).apply()
                },
                onToggleForceDarkMode = {
                    val newState = !isForceDarkMode
                    isForceDarkMode = newState
                    prefs.edit().putBoolean("force_dark_mode", newState).apply()
                },
                onToggleStealthMode = {
                    val newState = !isStealthMode
                    isStealthMode = newState
                    prefs.edit().putBoolean("stealth_mode", newState).apply()
                },
                onCustomSearchEngineUrlChanged = { url ->
                    customSearchEngineUrl = url
                    prefs.edit().putString("custom_search_engine_url", url).apply()
                },
                onCustomAdBlockerUrlChanged = { url ->
                    customAdBlockerUrl = url
                    prefs.edit().putString("custom_ad_blocker_url", url).apply()
                },
                onClearBrowsingData = {
                    android.webkit.WebView(context).clearCache(true)
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()
                },
                onOpenHistory = {
                    showSettingsScreen = false
                    showHistoryScreen = true
                },
                onOpenHowToUse = {
                    showSettingsScreen = false
                    showHowToUse = true
                }
            )
        }

        AnimatedVisibility(
            visible = showTabsScreen,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)),
            exit = fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
        ) {
            TabsScreen(
                tabList = tabs,
                onSelectTab = { id ->
                    val tab = tabs.find { it.id == id }
                    if (tab != null) {
                        currentUrl = tab.url
                        currentTitle = tab.title
                        showTabsScreen = false
                    }
                },
                onCloseTab = { id ->
                    tabs = tabs.filter { it.id != id }
                },
                onClearAllTabs = {
                    tabs = emptyList()
                },
                onAddTab = {
                    currentUrl = ""
                    showTabsScreen = false
                },
                onClose = { showTabsScreen = false }
            )
        }

        if (showHowToUse) {
            HowToUseScreen(
                onClose = { showHowToUse = false }
            )
        }
    }
}
