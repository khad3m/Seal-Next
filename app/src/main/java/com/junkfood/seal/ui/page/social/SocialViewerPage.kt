package com.junkfood.seal.ui.page.social

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.junkfood.seal.Downloader
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.AdBlockFilter
import com.junkfood.seal.util.LowRamOptimizer

enum class SocialPlatform(val label: String, val homeUrl: String, val accentHex: Long) {
    INSTAGRAM("Instagram", "https://www.instagram.com/", 0xFFE1306C),
    TIKTOK("TikTok", "https://www.tiktok.com/", 0xFF00C2CB),
    FACEBOOK("Facebook", "https://m.facebook.com/", 0xFF1877F2),
    YOUTUBE("YouTube", "https://m.youtube.com/", 0xFFFF0033),
    GOOGLE("Search", "https://www.google.com/", 0xFF4285F4),
}

private fun platformIcon(platform: SocialPlatform): ImageVector =
    when (platform) {
        SocialPlatform.YOUTUBE -> Icons.Outlined.SmartDisplay
        SocialPlatform.INSTAGRAM -> Icons.Outlined.PhotoCamera
        SocialPlatform.FACEBOOK -> Icons.Outlined.ThumbUp
        SocialPlatform.TIKTOK -> Icons.Outlined.MusicNote
        SocialPlatform.GOOGLE -> Icons.Outlined.Search
    }

private fun buildAntiDetectJs(chromiumMajor: String, fullVer: String, desktop: Boolean): String = """
(function() {
  'use strict';
  try {
    if (typeof window.chrome === 'undefined' || window.chrome === null) {
      var chromeDef = {
        app: { isInstalled: false },
        runtime: {
          connect: function() {
            return {
              postMessage: function() {}, disconnect: function() {},
              onMessage: { addListener: function() {}, removeListener: function() {} },
              onDisconnect: { addListener: function() {}, removeListener: function() {} }
            };
          },
          sendMessage: function() {},
          onMessage: { addListener: function() {}, removeListener: function() {}, hasListener: function() { return false; } }
        },
        loadTimes: function() { return {}; },
        csi: function() { return { startE: Date.now(), onload: Date.now(), onloadE: Date.now() }; }
      };
      try { Object.defineProperty(window, 'chrome', { value: chromeDef, writable: true, configurable: true }); }
      catch(e) { window.chrome = chromeDef; }
    }
    try { Object.defineProperty(navigator, 'webdriver', { get: function() { return false; }, configurable: true }); } catch(e) {}
    try {
      if (!navigator.languages || navigator.languages.length === 0) {
        Object.defineProperty(navigator, 'languages', { get: function() { return ['en-US', 'en']; }, configurable: true });
      }
    } catch(e) {}
    try {
      if (navigator.userAgentData) {
        var brands = [
          { brand: 'Not/A)Brand', version: '8' },
          { brand: 'Chromium', version: '$chromiumMajor' },
          { brand: 'Google Chrome', version: '$chromiumMajor' }
        ];
        var uaData = {
          brands: brands,
          mobile: ${!desktop},
          platform: '${if (desktop) "Linux" else "Android"}',
          getHighEntropyValues: function() {
            return Promise.resolve({
              brands: brands, mobile: ${!desktop},
              platform: '${if (desktop) "Linux" else "Android"}',
              platformVersion: '${Build.VERSION.RELEASE}',
              uaFullVersion: '$fullVer'
            });
          },
          toJSON: function() { return { brands: brands, mobile: ${!desktop}, platform: '${if (desktop) "Linux" else "Android"}' }; }
        };
        Object.defineProperty(navigator, 'userAgentData', { value: uaData, writable: true, configurable: true });
      }
    } catch(e) {}
    try {
      if (!window.__sealAdSkipper) {
        window.__sealAdSkipper = setInterval(function() {
          try {
            var skip = document.querySelector('.ytp-ad-skip-button, .ytp-skip-ad-button, .ytp-ad-skip-button-modern');
            if (skip) skip.click();
            var adPlaying = document.querySelector('.ad-showing video');
            if (adPlaying && isFinite(adPlaying.duration) && adPlaying.duration > 0) {
              adPlaying.muted = true;
              adPlaying.currentTime = adPlaying.duration;
            }
          } catch(e) {}
        }, 700);
      }
      var styleId = '__seal_native_feel__';
      if (document && document.head && !document.getElementById(styleId)) {
        var st = document.createElement('style');
        st.id = styleId;
        st.textContent = 'html, body { overscroll-behavior-y: contain; -webkit-tap-highlight-color: transparent; } ' +
          'ytm-app-upsell-dialog-renderer, .open-app-button, [data-testid="app-banner"] { display: none !important; }';
        document.head.appendChild(st);
      }
    } catch(e) {}
  } catch(err) {}
})();
""".trimIndent()

private fun isPlayableSocialVideoUrl(url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val host = uri.host?.lowercase().orEmpty()
    val path = uri.path?.lowercase().orEmpty()
    return when {
        host.contains("youtube.com") || host.contains("youtu.be") ->
            (path == "/watch" && !uri.getQueryParameter("v").isNullOrBlank()) ||
                path.startsWith("/shorts/") ||
                path.startsWith("/live/") ||
                host.contains("youtu.be")
        host.contains("instagram.com") || host.contains("instagr.am") ->
            path.startsWith("/reel/") || path.startsWith("/reels/") || path.startsWith("/tv/") || path.startsWith("/p/")
        host.contains("tiktok.com") ->
            path.contains("/video/") || host.startsWith("vm.") || host.startsWith("vt.")
        host.contains("facebook.com") || host.contains("fb.watch") || host.contains("fb.com") ->
            path.startsWith("/reel/") || path.startsWith("/watch") || path.contains("/videos/") || host.contains("fb.watch")
        else -> false
    }
}

// YouTube is native-only: the WebView must never host any youtube.com / youtu.be page.
private fun isYouTubeHostUrl(url: String): Boolean {
    val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull().orEmpty()
    return host.contains("youtube.com") || host.contains("youtu.be")
}

private fun normalizeBrowserInput(raw: String): String {
    val t = raw.trim()
    if (t.startsWith("http://") || t.startsWith("https://")) return t
    return if (t.contains('.') && !t.contains(' ')) "https://$t"
    else "https://www.google.com/search?q=${Uri.encode(t)}"
}

private fun extractDisplayHost(url: String): String =
    runCatching { Uri.parse(url).host?.removePrefix("www.")?.removePrefix("m.") }.getOrNull()
        ?.takeIf { it.isNotBlank() } ?: url

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SocialViewerPage(
    onNavigateBack: () -> Unit,
    onOpenInPlayer: (String) -> Unit,
    initialPlatform: SocialPlatform = SocialPlatform.INSTAGRAM,
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current && isDarkTheme

    var currentUrl by remember { mutableStateOf(initialPlatform.homeUrl) }
    var pageTitle by remember { mutableStateOf(initialPlatform.label) }
    var addressText by remember { mutableStateOf(initialPlatform.homeUrl) }
    var addressFocused by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var adBlockEnabled by remember { mutableStateOf(true) }
    var blockedCount by remember { mutableIntStateOf(0) }
    var desktopMode by remember { mutableStateOf(false) }
    var showStreamsSheet by remember { mutableStateOf(false) }
    var showAdBlockDialog by remember { mutableStateOf(false) }
    var showTabsSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var isBookmarked by remember(currentUrl) { mutableStateOf(BrowserBookmarkStore.isBookmarked(currentUrl)) }
    var bookmarks by remember { mutableStateOf(BrowserBookmarkStore.getAll()) }

    val detectedStreams = remember { mutableStateListOf<String>() }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val tabs = remember {
        mutableStateListOf(BrowserTab(id = 1L, title = initialPlatform.label, url = initialPlatform.homeUrl))
    }
    var activeTabId by remember { mutableLongStateOf(1L) }

    var browsing by remember { mutableStateOf(false) }
    var manualAddress by remember { mutableStateOf("") }
    var nativePlayerIntercept by remember { mutableStateOf(true) }
    var lastInterceptedUrl by remember { mutableStateOf("") }
    var lastInterceptedMs by remember { mutableLongStateOf(0L) }

    fun updateActiveTab(url: String, title: String? = null) {
        val idx = tabs.indexOfFirst { it.id == activeTabId }
        if (idx >= 0) {
            val prev = tabs[idx]
            tabs[idx] = prev.copy(
                url = url,
                title = title?.takeIf { it.isNotBlank() } ?: extractDisplayHost(url),
            )
        }
    }

    fun maybeInterceptToNativePlayer(url: String, fromSpa: Boolean, wv: WebView?): Boolean {
        // YouTube never runs inside the WebView: every youtube.com / youtu.be URL —
        // watch, shorts, channel, search or home — goes to the native player page no
        // matter what the Web Player toggle says. FB / IG / TikTok keep the toggle.
        val youTube = isYouTubeHostUrl(url)
        if (!youTube) {
            if (!nativePlayerIntercept) return false
            if (!isPlayableSocialVideoUrl(url)) return false
        }
        val now = System.currentTimeMillis()
        if (url == lastInterceptedUrl && now - lastInterceptedMs < 3000L) return false
        lastInterceptedUrl = url
        lastInterceptedMs = now
        wv?.onPause()
        if (fromSpa && wv?.canGoBack() == true) {
            wv.goBack()
        }
        onOpenInPlayer(url)
        return true
    }

    val realUa = remember(desktopMode) { AdBlockFilter.buildRealDeviceUA(context, desktopMode) }
    val antiDetectJs = remember(realUa, desktopMode) {
        buildAntiDetectJs(realUa.chromiumMajor, realUa.fullVersion, desktopMode)
    }

    BackHandler(enabled = customVideoView != null || browsing || canGoBack) {
        when {
            customVideoView != null -> {
                customViewCallback?.onCustomViewHidden()
                customVideoView = null
                customViewCallback = null
            }
            browsing && webViewRef?.canGoBack() == true -> webViewRef?.goBack()
            browsing -> {
                browsing = false
                webViewRef?.stopLoading()
            }
            else -> onNavigateBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                onPause()
                clearCache(false)
                clearHistory()
                destroy()
            }
            webViewRef = null
        }
    }

    fun applyUaAndMetadata(wv: WebView) {
        wv.settings.userAgentString = realUa.userAgent
        if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
            val metadata =
                UserAgentMetadata.Builder()
                    .setBrandVersionList(
                        listOf(
                            UserAgentMetadata.BrandVersion.Builder()
                                .setBrand("Google Chrome")
                                .setMajorVersion(realUa.chromiumMajor)
                                .setFullVersion(realUa.fullVersion)
                                .build(),
                            UserAgentMetadata.BrandVersion.Builder()
                                .setBrand("Chromium")
                                .setMajorVersion(realUa.chromiumMajor)
                                .setFullVersion(realUa.fullVersion)
                                .build(),
                        )
                    )
                    .setMobile(!desktopMode)
                    .setPlatform(if (desktopMode) "Linux" else "Android")
                    .build()
            WebSettingsCompat.setUserAgentMetadata(wv.settings, metadata)
        }
    }

    val backgroundColor =
        if (isGradientDark) GradientDarkColors.Background else MaterialTheme.colorScheme.background

    fun openUrlInActiveTab(url: String, title: String? = null) {
        pageError = null
        currentUrl = url
        addressText = url
        pageTitle = title ?: extractDisplayHost(url)
        detectedStreams.clear()
        updateActiveTab(url, title)
        browsing = true
        webViewRef?.onResume()
        webViewRef?.loadUrl(url)
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---------- Chrome-like Top Bar ----------
            AnimatedVisibility(
                visible = browsing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(backgroundColor)
                            .statusBarsPadding()
                            .padding(top = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            tonalElevation = 3.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.weight(1f).height(42.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = "AdBlock",
                                    tint =
                                        if (adBlockEnabled) Color(0xFF4CAF50)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                        Modifier.size(18.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    adBlockEnabled = !adBlockEnabled
                                                    Toast.makeText(
                                                        context,
                                                        if (adBlockEnabled) "AdGuard Filter ON" else "AdGuard Filter OFF",
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                },
                                                onLongClick = { showAdBlockDialog = true },
                                            ),
                                )
                                if (adBlockEnabled && blockedCount > 0) {
                                    Text(
                                        text = "$blockedCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                if (!addressFocused && currentUrl.startsWith("https://")) {
                                    Icon(
                                        Icons.Outlined.Lock,
                                        contentDescription = "HTTPS",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                                BasicTextField(
                                    value = if (addressFocused) addressText else extractDisplayHost(currentUrl),
                                    onValueChange = { addressText = it },
                                    singleLine = true,
                                    textStyle =
                                        MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (addressFocused) FontWeight.Normal else FontWeight.Medium,
                                        ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                    keyboardActions =
                                        KeyboardActions(
                                            onGo = {
                                                keyboard?.hide()
                                                focusManager.clearFocus()
                                                openUrlInActiveTab(normalizeBrowserInput(addressText))
                                            }
                                        ),
                                    modifier =
                                        Modifier.weight(1f).onFocusChanged { state ->
                                            addressFocused = state.isFocused
                                            if (state.isFocused) addressText = currentUrl
                                        },
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                isBookmarked = BrowserBookmarkStore.toggle(currentUrl, pageTitle)
                                bookmarks = BrowserBookmarkStore.getAll()
                                Toast.makeText(
                                    context,
                                    if (isBookmarked) "Bookmarked" else "Bookmark removed",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                nativePlayerIntercept = !nativePlayerIntercept
                                Toast.makeText(
                                    context,
                                    if (nativePlayerIntercept) "Native Ad-Free Player ON" else "Web Player Mode",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Outlined.PlayCircle,
                                contentDescription = "Native Player Mode",
                                tint =
                                    if (nativePlayerIntercept) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                pageError = null
                                webViewRef?.reload()
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Reload", modifier = Modifier.size(19.dp))
                        }
                    }

                    // Inline horizontal tab strip
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item(key = "add_tab") {
                            Surface(
                                onClick = {
                                    val nextId = (tabs.maxOfOrNull { it.id } ?: 0L) + 1L
                                    tabs.add(BrowserTab(id = nextId, title = "New Tab", url = "https://www.google.com/"))
                                    activeTabId = nextId
                                    openUrlInActiveTab("https://www.google.com/", "Search")
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = "New tab", modifier = Modifier.padding(6.dp).size(16.dp))
                            }
                        }
                        items(tabs, key = { it.id }) { tab ->
                            val active = tab.id == activeTabId
                            Surface(
                                shape = CircleShape,
                                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.combinedClickable(
                                    onClick = {
                                        activeTabId = tab.id
                                        openUrlInActiveTab(tab.url, tab.title)
                                    },
                                    onLongClick = {
                                        if (tabs.size > 1) {
                                            tabs.remove(tab)
                                            if (active) {
                                                val fallback = tabs.first()
                                                activeTabId = fallback.id
                                                openUrlInActiveTab(fallback.url, fallback.title)
                                            }
                                        }
                                    },
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (tabs.size > 1) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Close tab",
                                            modifier = Modifier.size(13.dp).clickable {
                                                tabs.remove(tab)
                                                if (active) {
                                                    val fallback = tabs.first()
                                                    activeTabId = fallback.id
                                                    openUrlInActiveTab(fallback.url, fallback.title)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = progress in 1..99, enter = fadeIn(), exit = fadeOut()) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                        )
                    }
                }
            }

            // ---------- WebView / Error layer ----------
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(AndroidColor.BLACK)
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            val cm = CookieManager.getInstance()
                            cm.setAcceptCookie(true)
                            cm.setAcceptThirdPartyCookies(this, true)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mediaPlaybackRequiresUserGesture = LowRamOptimizer.isLowRamDevice(ctx)
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            }
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.OFF_SCREEN_PRERASTER)) {
                                WebSettingsCompat.setOffscreenPreRaster(settings, true)
                            }
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDarkTheme)
                            }
                            applyUaAndMetadata(this)

                            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                                WebViewCompat.addDocumentStartJavaScript(this, antiDetectJs, setOf("*"))
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank()) {
                                        pageTitle = title
                                        updateActiveTab(currentUrl, title)
                                    }
                                }

                                override fun onPermissionRequest(request: PermissionRequest?) {
                                    request?.deny()
                                }

                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    if (customVideoView != null) {
                                        callback?.onCustomViewHidden()
                                        return
                                    }
                                    customVideoView = view
                                    customViewCallback = callback
                                }

                                override fun onHideCustomView() {
                                    customViewCallback?.onCustomViewHidden()
                                    customVideoView = null
                                    customViewCallback = null
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): WebResourceResponse? {
                                    val reqUrl = request?.url?.toString() ?: return null
                                    if (adBlockEnabled && AdBlockFilter.isAdOrTracker(reqUrl, currentUrl)) {
                                        blockedCount++
                                        AdBlockFilter.incrementTotalAdsBlocked()
                                        return AdBlockFilter.emptyAdResponse()
                                    }
                                    if (AdBlockFilter.isDirectMediaStream(reqUrl) &&
                                        reqUrl !in detectedStreams &&
                                        detectedStreams.size < 10
                                    ) {
                                        detectedStreams.add(reqUrl)
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    val scheme = Uri.parse(url).scheme?.lowercase()
                                    if (scheme == "http" || scheme == "https") {
                                        if (maybeInterceptToNativePlayer(url, fromSpa = false, wv = view)) {
                                            return true
                                        }
                                        pageError = null
                                        currentUrl = url
                                        addressText = url
                                        updateActiveTab(url)
                                        return false
                                    }
                                    return true
                                }

                                override fun doUpdateVisitedHistory(
                                    view: WebView?,
                                    url: String?,
                                    isReload: Boolean,
                                ) {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                    if (!url.isNullOrBlank()) {
                                        if (!maybeInterceptToNativePlayer(url, fromSpa = true, wv = view)) {
                                            currentUrl = url
                                            addressText = url
                                            updateActiveTab(url)
                                        }
                                    }
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (!url.isNullOrBlank()) {
                                        currentUrl = url
                                        addressText = url
                                        val title = view?.title.orEmpty()
                                        updateActiveTab(url, title)
                                        BrowserHistoryStore.recordVisit(url, title)
                                    }
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                    view?.evaluateJavascript(antiDetectJs, null)
                                    CookieManager.getInstance().flush()
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?,
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        pageError = error?.description?.toString() ?: "Unable to load page"
                                    }
                                }
                            }

                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Page Loading Skeleton (initial load)
                if (browsing && progress in 1..28 && pageError == null) {
                    Surface(color = backgroundColor, modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            )
                            repeat(4) { idx ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (idx % 2 == 0) 0.9f else 0.65f)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                )
                            }
                        }
                    }
                }

                // Offline / Network Error State
                if (browsing && pageError != null) {
                    Surface(
                        color = backgroundColor,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Outlined.WifiOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Page couldn't load",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = pageError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    pageError = null
                                    webViewRef?.reload()
                                },
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            // ---------- One-Hand Bottom Navigation Bar ----------
            AnimatedVisibility(visible = browsing) {
                Surface(
                    tonalElevation = 4.dp,
                    color = backgroundColor,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                ) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { webViewRef?.goBack() }, enabled = canGoBack) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                            }
                            IconButton(onClick = { webViewRef?.goForward() }, enabled = canGoForward) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Forward")
                            }
                            IconButton(onClick = {
                                browsing = false
                                webViewRef?.stopLoading()
                            }) {
                                Icon(Icons.Outlined.Home, contentDescription = "Browser Home")
                            }
                            TextButton(onClick = { showTabsSheet = true }) {
                                Icon(Icons.Outlined.Layers, contentDescription = "Tabs", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${tabs.size}", fontWeight = FontWeight.SemiBold)
                            }
                            if (detectedStreams.isNotEmpty()) {
                                FilledTonalButton(
                                    onClick = { showStreamsSheet = true },
                                    shape = CircleShape,
                                ) {
                                    Icon(Icons.Outlined.VideoFile, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${detectedStreams.size}")
                                }
                            }
                            Box {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "Menu")
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Watch in Native Player") },
                                        leadingIcon = { Icon(Icons.Outlined.PlayCircle, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            onOpenInPlayer(currentUrl)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Download from Page") },
                                        leadingIcon = { Icon(Icons.Outlined.Download, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            Downloader.getInfoAndDownload(currentUrl)
                                            Toast.makeText(context, "Downloading from page...", Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Copy Link") },
                                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
                                            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (desktopMode) "Mobile Site" else "Desktop Site") },
                                        leadingIcon = {
                                            Icon(
                                                if (desktopMode) Icons.Outlined.Smartphone else Icons.Outlined.DesktopWindows,
                                                null,
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            desktopMode = !desktopMode
                                            webViewRef?.let {
                                                applyUaAndMetadata(it)
                                                it.reload()
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Browser History") },
                                        leadingIcon = { Icon(Icons.Outlined.History, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showHistorySheet = true
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("AdBlock Rules") },
                                        leadingIcon = { Icon(Icons.Outlined.Shield, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showAdBlockDialog = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------- Browser Home Screen ----------
        AnimatedVisibility(
            visible = !browsing,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            BrowserHomeScreen(
                isGradientDark = isGradientDark,
                manualAddress = manualAddress,
                tabCount = tabs.size,
                bookmarks = bookmarks,
                onManualAddressChange = { manualAddress = it },
                onOpenPlatform = { platform ->
                    if (platform == SocialPlatform.YOUTUBE) {
                        // YouTube is native-only — open the player page, never the WebView.
                        onOpenInPlayer(platform.homeUrl)
                    } else {
                        openUrlInActiveTab(platform.homeUrl, platform.label)
                    }
                },
                onOpenAddress = { raw ->
                    keyboard?.hide()
                    openUrlInActiveTab(normalizeBrowserInput(raw))
                },
                onOpenHistory = { showHistorySheet = true },
                onOpenTabs = { showTabsSheet = true },
                onNavigateBack = onNavigateBack,
            )
        }

        customVideoView?.let { fsView ->
            val activity = context as? Activity
            DisposableEffect(fsView) {
                val window = activity?.window
                val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
                controller?.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller?.hide(WindowInsetsCompat.Type.systemBars())
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                onDispose {
                    controller?.show(WindowInsetsCompat.Type.systemBars())
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = {
                        (fsView.parent as? ViewGroup)?.removeView(fsView)
                        fsView
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (showTabsSheet) {
        ModalBottomSheet(onDismissRequest = { showTabsSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Open Tabs (${tabs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FilledTonalButton(
                        onClick = {
                            val nextId = (tabs.maxOfOrNull { it.id } ?: 0L) + 1L
                            tabs.add(BrowserTab(id = nextId, title = "New Tab", url = "https://www.google.com/"))
                            activeTabId = nextId
                            showTabsSheet = false
                            openUrlInActiveTab("https://www.google.com/", "Search")
                        },
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Tab")
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabs, key = { it.id }) { tab ->
                        val active = tab.id == activeTabId
                        Surface(
                            onClick = {
                                activeTabId = tab.id
                                showTabsSheet = false
                                openUrlInActiveTab(tab.url, tab.title)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color =
                                if (active) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tab.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(extractDisplayHost(tab.url), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (tabs.size > 1) {
                                    IconButton(onClick = {
                                        tabs.remove(tab)
                                        if (active) {
                                            val fallback = tabs.first()
                                            activeTabId = fallback.id
                                            openUrlInActiveTab(fallback.url, fallback.title)
                                        }
                                    }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Close tab", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showHistorySheet) {
        var historyQuery by remember { mutableStateOf("") }
        var showMostVisited by remember { mutableStateOf(false) }
        var entries by remember { mutableStateOf(BrowserHistoryStore.getAll()) }
        val filtered = remember(entries, historyQuery, showMostVisited) {
            entries
                .filter {
                    historyQuery.isBlank() ||
                        it.title.contains(historyQuery, ignoreCase = true) ||
                        it.url.contains(historyQuery, ignoreCase = true)
                }
                .let { list ->
                    if (showMostVisited) list.sortedByDescending { it.visitCount }
                    else list.sortedByDescending { it.lastVisitedMs }
                }
        }
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Browser History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = {
                            BrowserHistoryStore.clear()
                            entries = emptyList()
                        }) {
                            Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                }
                OutlinedTextField(
                    value = historyQuery,
                    onValueChange = { historyQuery = it },
                    placeholder = { Text("Search history...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showMostVisited,
                        onClick = { showMostVisited = false },
                        label = { Text("Recently visited") },
                    )
                    FilterChip(
                        selected = showMostVisited,
                        onClick = { showMostVisited = true },
                        label = { Text("Most visited") },
                    )
                }
                if (filtered.isEmpty()) {
                    Text("No history yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filtered, key = { it.url }) { item ->
                            Surface(
                                onClick = {
                                    showHistorySheet = false
                                    openUrlInActiveTab(item.url, item.title)
                                },
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${extractDisplayHost(item.url)} • ${item.visitCount} visits", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    if (showAdBlockDialog) {
        var customDomain by remember { mutableStateOf("") }
        var whitelisted by remember(currentUrl) { mutableStateOf(AdBlockFilter.isHostWhitelisted(currentUrl)) }
        AlertDialog(
            onDismissRequest = { showAdBlockDialog = false },
            title = { Text("AdGuard Filter & Rules") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Blocked $blockedCount requests this session.", style = MaterialTheme.typography.bodySmall)
                    FilledTonalButton(
                        onClick = {
                            whitelisted = AdBlockFilter.toggleWhitelistHost(currentUrl)
                            webViewRef?.reload()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (whitelisted) "Remove Site from Whitelist" else "Whitelist Current Site")
                    }
                    OutlinedTextField(
                        value = customDomain,
                        onValueChange = { customDomain = it },
                        label = { Text("Add custom block domain") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customDomain.isNotBlank()) {
                            AdBlockFilter.addCustomBlockedDomain(customDomain)
                            Toast.makeText(context, "Added to blocklist", Toast.LENGTH_SHORT).show()
                        }
                        showAdBlockDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAdBlockDialog = false }) { Text("Close") }
            },
        )
    }

    if (showStreamsSheet) {
        ModalBottomSheet(onDismissRequest = { showStreamsSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Detected Streams (${detectedStreams.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(detectedStreams.toList()) { streamUrl ->
                        Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(streamUrl, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    showStreamsSheet = false
                                    onOpenInPlayer(streamUrl)
                                }) {
                                    Icon(Icons.Outlined.PlayCircle, contentDescription = "Play")
                                }
                                IconButton(onClick = {
                                    Downloader.getInfoAndDownload(streamUrl)
                                    Toast.makeText(context, "Stream download started", Toast.LENGTH_SHORT).show()
                                    showStreamsSheet = false
                                }) {
                                    Icon(Icons.Outlined.Download, contentDescription = "Download")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserHomeScreen(
    isGradientDark: Boolean,
    manualAddress: String,
    tabCount: Int,
    bookmarks: List<BrowserBookmark>,
    onManualAddressChange: (String) -> Unit,
    onOpenPlatform: (SocialPlatform) -> Unit,
    onOpenAddress: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTabs: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(
                    if (isGradientDark) {
                        Brush.verticalGradient(listOf(GradientDarkColors.Background, Color.Black))
                    } else {
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceContainerLowest)
                        )
                    }
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                Icon(
                    Icons.Outlined.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp).size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Browser", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Quick access to supported sites", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onOpenHistory) {
                Icon(Icons.Outlined.History, contentDescription = "History")
            }
            TextButton(onClick = onOpenTabs) {
                Icon(Icons.Outlined.Layers, contentDescription = "Tabs", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("$tabCount")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SocialPlatform.entries
                .filter { it != SocialPlatform.GOOGLE && it != SocialPlatform.YOUTUBE }
                .chunked(2)
                .forEach { rowPlatforms ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowPlatforms.forEach { platform ->
                        PlatformCard(
                            platform = platform,
                            isDarkTheme = isDarkTheme,
                            isGradientDark = isGradientDark,
                            onClick = { onOpenPlatform(platform) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowPlatforms.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 3.dp,
            color =
                if (isGradientDark) GradientDarkColors.GlassSurface.copy(alpha = 0.06f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                Modifier.fillMaxWidth().then(
                    if (isGradientDark) {
                        Modifier.border(1.dp, GradientDarkColors.GlassWhiteBorder, RoundedCornerShape(18.dp))
                    } else Modifier
                ),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Open any link", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                OutlinedTextField(
                    value = manualAddress,
                    onValueChange = onManualAddressChange,
                    placeholder = { Text("Type an address or search...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (manualAddress.isNotBlank()) onOpenAddress(manualAddress) }),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    trailingIcon = {
                        IconButton(
                            onClick = { if (manualAddress.isNotBlank()) onOpenAddress(manualAddress) },
                            enabled = manualAddress.isNotBlank(),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Go")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (bookmarks.isNotEmpty()) {
            Text(
                text = "Bookmarks",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bookmarks, key = { it.url }) { bm ->
                    FilterChip(
                        selected = false,
                        onClick = { onOpenAddress(bm.url) },
                        leadingIcon = { Icon(Icons.Filled.Star, null, modifier = Modifier.size(14.dp)) },
                        label = { Text(bm.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Back to Seal Next")
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun PlatformCard(
    platform: SocialPlatform,
    isDarkTheme: Boolean,
    isGradientDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(),
        label = "platform_card_scale",
    )
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp,
        color =
            if (isGradientDark) GradientDarkColors.GlassSurface.copy(alpha = 0.06f)
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier =
            modifier
                .height(120.dp)
                .scale(pressedScale)
                .then(
                    if (isGradientDark) {
                        Modifier.border(1.dp, GradientDarkColors.GlassWhiteBorder, RoundedCornerShape(20.dp))
                    } else Modifier
                ),
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(platform.accentHex).copy(alpha = if (isGradientDark) 0.16f else 0.10f),
                                Color.Transparent,
                            )
                        )
                    )
                    .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(shape = CircleShape, color = Color(platform.accentHex).copy(alpha = 0.16f)) {
                Icon(
                    platformIcon(platform),
                    contentDescription = platform.label,
                    tint = Color(platform.accentHex),
                    modifier = Modifier.padding(10.dp).size(26.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = platform.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
