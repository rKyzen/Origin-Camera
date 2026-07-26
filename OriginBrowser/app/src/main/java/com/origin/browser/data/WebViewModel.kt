package com.origin.browser.data

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HistoryItem(
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class BookmarkItem(
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BrowserDialogType {
    NONE,
    HISTORY,
    DELETE_DATA,
    SITE_CONTROLS,
    DOWNLOADS,
    BOOKMARKS,
    RECENT_TABS,
    FIND_IN_PAGE
}

data class WebUiState(
    val currentUrl: String = "",
    val inputUrl: String = "",
    val pageTitle: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isHome: Boolean = true,

    // Site Controls
    val isDesktopMode: Boolean = false,
    val isJavaScriptEnabled: Boolean = true,

    // Collections
    val history: List<HistoryItem> = emptyList(),
    val bookmarks: List<BookmarkItem> = emptyList(),
    val recentTabs: List<String> = emptyList(),

    // UI Dialog & Find State
    val activeDialog: BrowserDialogType = BrowserDialogType.NONE,
    val findQuery: String = ""
)

class WebViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WebUiState())
    val uiState: StateFlow<WebUiState> = _uiState.asStateFlow()

    fun onInputUrlChange(newInput: String) {
        _uiState.update { it.copy(inputUrl = newInput) }
    }

    fun onFindQueryChange(newQuery: String) {
        _uiState.update { it.copy(findQuery = newQuery) }
    }

    fun openDialog(dialog: BrowserDialogType) {
        _uiState.update { it.copy(activeDialog = dialog) }
    }

    fun closeDialog() {
        _uiState.update { it.copy(activeDialog = BrowserDialogType.NONE, findQuery = "") }
    }

    fun toggleDesktopMode() {
        _uiState.update { it.copy(isDesktopMode = !it.isDesktopMode) }
    }

    fun toggleJavaScript() {
        _uiState.update { it.copy(isJavaScriptEnabled = !it.isJavaScriptEnabled) }
    }

    fun addBookmark(url: String, title: String) {
        if (url.isBlank()) return
        _uiState.update { state ->
            if (state.bookmarks.none { it.url == url }) {
                state.copy(bookmarks = listOf(BookmarkItem(url, title.ifBlank { url })) + state.bookmarks)
            } else {
                state
            }
        }
    }

    fun removeBookmark(url: String) {
        _uiState.update { state ->
            state.copy(bookmarks = state.bookmarks.filterNot { it.url == url })
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
    }

    fun clearBrowsingData() {
        _uiState.update {
            it.copy(
                history = emptyList(),
                currentUrl = "",
                inputUrl = "",
                pageTitle = "",
                isHome = true
            )
        }
    }

    fun submitQuery(query: String) {
        val formattedUrl = resolveQueryToUrl(query)
        if (formattedUrl.isNotBlank()) {
            _uiState.update {
                it.copy(
                    currentUrl = formattedUrl,
                    inputUrl = formattedUrl,
                    isHome = false,
                    isLoading = true,
                    progress = 10,
                    activeDialog = BrowserDialogType.NONE
                )
            }
        }
    }

    fun goHome() {
        _uiState.update {
            it.copy(
                isHome = true,
                currentUrl = "",
                inputUrl = "",
                pageTitle = "",
                isLoading = false,
                progress = 0,
                activeDialog = BrowserDialogType.NONE
            )
        }
    }

    fun updateProgress(progress: Int) {
        _uiState.update {
            it.copy(
                progress = progress,
                isLoading = progress < 100
            )
        }
    }

    fun updatePageState(url: String, title: String, canBack: Boolean, canForward: Boolean) {
        if (url.isBlank()) return
        _uiState.update { state ->
            val updatedHistory = if (state.history.firstOrNull()?.url != url) {
                listOf(HistoryItem(url, title.ifBlank { url })) + state.history
            } else {
                state.history
            }
            val updatedRecentTabs = if (!state.recentTabs.contains(url)) {
                listOf(url) + state.recentTabs
            } else {
                state.recentTabs
            }

            state.copy(
                currentUrl = url,
                inputUrl = url,
                pageTitle = title,
                canGoBack = canBack,
                canGoForward = canForward,
                isHome = false,
                history = updatedHistory,
                recentTabs = updatedRecentTabs
            )
        }
    }

    private fun resolveQueryToUrl(query: String): String {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return ""

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }

        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return "https://$trimmed"
        }

        return "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
    }
}
