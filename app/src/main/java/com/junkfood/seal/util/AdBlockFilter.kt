package com.junkfood.seal.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewCompat
import com.tencent.mmkv.MMKV
import java.io.ByteArrayInputStream

/**
 * Zero-bloat AdGuard/EasyList-inspired domain blocker + real-device Chrome fingerprint helper
 * + lightweight media stream sniffer for System WebView.
 */
object AdBlockFilter {

    // Compact O(1) HashSet of common ad, tracker, analytics, and popup domains (~0.5 MB RAM).
    private val blockedDomains: HashSet<String> = hashSetOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "criteo.net",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "appnexus.com",
        "adnxs.com",
        "casalemedia.com",
        "advertising.com",
        "adroll.com",
        "adsrvr.org",
        "smartadserver.com",
        "moatads.com",
        "scorecardresearch.com",
        "quantserve.com",
        "hotjar.com",
        "mixpanel.com",
        "amplitude.com",
        "segment.io",
        "segment.com",
        "omtrdc.net",
        "demdex.net",
        "bluekai.com",
        "krxd.net",
        "exelator.com",
        "eyeota.net",
        "bidswitch.net",
        "yieldmo.com",
        "sharethrough.com",
        "triplelift.com",
        "indexexchange.com",
        "spotxchange.com",
        "teads.tv",
        "mgid.com",
        "revcontent.com",
        "propellerads.com",
        "popads.net",
        "popcash.net",
        "adsterra.com",
        "juicyads.com",
        "exoclick.com",
        "trafficjunky.com",
        "unityads.unity3d.com",
        "adcolony.com",
        "chartboost.com",
        "vungle.com",
        "applovin.com",
        "ironsrc.com",
        "branch.io",
        "adjust.com",
        "appsflyer.com",
        "kochava.com",
        "bugsnag.com",
        "sentry.io",
        "newrelic.com",
        "nr-data.net",
        "analytics.tiktok.com",
        "pixel.facebook.com",
        "an.facebook.com",
        "amazon-adsystem.com",
        "ads-twitter.com",
        "analytics.twitter.com",
        "ads.linkedin.com",
        "snap.licdn.com",
        "bat.bing.com",
        "clarity.ms",
    )

    private const val KEY_WHITELIST = "adblock_whitelist_hosts"
    private const val KEY_CUSTOM_BLOCK = "adblock_custom_domains"
    private const val KEY_TOTAL_BLOCKED = "adblock_total_blocked"

    fun getTotalAdsBlocked(): Long =
        runCatching { MMKV.defaultMMKV().decodeLong(KEY_TOTAL_BLOCKED, 0L) }.getOrDefault(0L)

    fun incrementTotalAdsBlocked() {
        runCatching {
            val mmkv = MMKV.defaultMMKV()
            mmkv.encode(KEY_TOTAL_BLOCKED, mmkv.decodeLong(KEY_TOTAL_BLOCKED, 0L) + 1L)
        }
    }

    fun getWhitelistHosts(): Set<String> =
        runCatching { MMKV.defaultMMKV().decodeStringSet(KEY_WHITELIST, emptySet()).orEmpty() }.getOrDefault(emptySet())

    fun isHostWhitelisted(pageUrl: String): Boolean {
        val host = runCatching { Uri.parse(pageUrl).host?.lowercase() }.getOrNull() ?: return false
        return host in getWhitelistHosts()
    }

    fun toggleWhitelistHost(pageUrl: String): Boolean {
        val host = runCatching { Uri.parse(pageUrl).host?.lowercase() }.getOrNull() ?: return false
        val current = getWhitelistHosts().toMutableSet()
        val added = if (host in current) { current.remove(host); false } else { current.add(host); true }
        runCatching { MMKV.defaultMMKV().encode(KEY_WHITELIST, current) }
        return added
    }

    fun getCustomBlockedDomains(): Set<String> =
        runCatching { MMKV.defaultMMKV().decodeStringSet(KEY_CUSTOM_BLOCK, emptySet()).orEmpty() }.getOrDefault(emptySet())

    fun addCustomBlockedDomain(domain: String) {
        val clean = domain.trim().lowercase().removePrefix("https://").removePrefix("http://").substringBefore("/")
        if (clean.isBlank()) return
        val current = getCustomBlockedDomains().toMutableSet().apply { add(clean) }
        runCatching { MMKV.defaultMMKV().encode(KEY_CUSTOM_BLOCK, current) }
    }

    fun isAdOrTracker(url: String, pageUrl: String = ""): Boolean {
        if (pageUrl.isNotBlank() && isHostWhitelisted(pageUrl)) return false
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
        val custom = getCustomBlockedDomains()
        if (host in blockedDomains || host in custom) return true
        // Walk parent domains: a.b.doubleclick.net -> b.doubleclick.net -> doubleclick.net
        var idx = host.indexOf('.')
        while (idx != -1) {
            val parent = host.substring(idx + 1)
            if (parent in blockedDomains || parent in custom) return true
            idx = host.indexOf('.', idx + 1)
        }
        return false
    }

    fun emptyAdResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))

    fun isDirectMediaStream(url: String): Boolean {
        val lower = url.lowercase()
        // Skip tiny HLS/DASH media segments
        if (lower.contains(".ts?") || lower.endsWith(".ts") || lower.contains(".m4s") || lower.contains("init.mp4")) {
            return false
        }
        return lower.contains(".m3u8") ||
            lower.contains(".mpd") ||
            (lower.contains(".mp4") && !lower.contains("thumb")) ||
            lower.contains(".webm") ||
            lower.contains("mime=video") ||
            lower.contains("videoplayback")
    }

    data class RealDeviceUA(val userAgent: String, val chromiumMajor: String, val fullVersion: String)

    fun buildRealDeviceUA(context: Context, desktopMode: Boolean = false): RealDeviceUA {
        val pkgVersion =
            runCatching { WebViewCompat.getCurrentWebViewPackage(context)?.versionName }.getOrNull()
        val fullVer = pkgVersion?.takeIf { it.contains('.') } ?: "136.0.0.0"
        val major = fullVer.substringBefore('.').ifBlank { "136" }
        val ua =
            if (desktopMode) {
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$fullVer Safari/537.36"
            } else {
                val release = Build.VERSION.RELEASE.ifBlank { "14" }
                val model = Build.MODEL.ifBlank { "Pixel 8" }
                "Mozilla/5.0 (Linux; Android $release; $model) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$fullVer Mobile Safari/537.36"
            }
        return RealDeviceUA(userAgent = ua, chromiumMajor = major, fullVersion = fullVer)
    }
}
