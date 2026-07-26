package com.origin.browser.data.adblock

import android.net.Uri

object AdBlocker {
    private val BLOCKED_DOMAINS = hashSetOf(
        "doubleclick.net",
        "google-analytics.com",
        "googlesyndication.com",
        "googleadservices.com",
        "googletagservices.com",
        "facebook.net",
        "connect.facebook.net",
        "pixel.facebook.com",
        "adnxs.com",
        "amazon-adsystem.com",
        "adzerk.net",
        "criteo.com",
        "criteo.net",
        "outbrain.com",
        "taboola.com",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "casalemedia.com",
        "indexww.com",
        "smartadserver.com",
        "moatads.com",
        "scorecardresearch.com",
        "quantserve.com",
        "buysellads.com",
        "adroll.com",
        "chartbeat.com",
        "hotjar.com",
        "clarity.ms",
        "segment.io",
        "mixpanel.com",
        "amplitude.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "exoclick.com",
        "juicyads.com",
        "adform.net",
        "yieldmo.com",
        "teads.tv",
        "media.net",
        "revcontent.com",
        "mgid.com",
        "infolinks.com",
        "bidswitch.net",
        "triplelift.com",
        "spotxchange.com"
    )

    fun shouldBlock(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            var currentHost = host
            while (currentHost.contains(".")) {
                if (BLOCKED_DOMAINS.contains(currentHost)) {
                    return true
                }
                val index = currentHost.indexOf(".")
                if (index != -1 && index < currentHost.length - 1) {
                    currentHost = currentHost.substring(index + 1)
                } else {
                    break
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
