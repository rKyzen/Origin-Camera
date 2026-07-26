package com.origin.browser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.origin.browser.ui.theme.OriginBrowserTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val url = extractUrl(intent)
        setContent {
            OriginBrowserTheme {
                OriginBrowserApp(initialUrl = url)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = extractUrl(intent)
        setContent {
            OriginBrowserTheme {
                OriginBrowserApp(initialUrl = url)
            }
        }
    }

    private fun extractUrl(intent: Intent?): String {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            return intent.data.toString()
        }
        return ""
    }
}
