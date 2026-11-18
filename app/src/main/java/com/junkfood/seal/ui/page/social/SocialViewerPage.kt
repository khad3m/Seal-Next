package com.junkfood.seal.ui.page.social

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
        csi: function() { return { startE: Date.now(), onloadT: Date.now(), pageT: 0, tran: 15 }; }
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
  } catch(err) {}
})();
""".trimIndent()

private fun normalizeBrowserInput(raw: String): String {
    val t = raw.trim()
    if (t.startsWith("http://") || t.startsWith("https://")) return t
    return if (t.contains('.') && !t.contains(' ')) "https://$t"
    else "https://www.google.com/search?q=${Uri.encode(t)}"
}

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
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current && isDarkTheme

    var selectedPlatform by remember { mutableStateOf(initialPlatform) }
    var currentUrl by remember { mutableStateOf(initialPlatform.homeUrl) }
    var addressText by remember { mutableStateOf(initialPlatform.homeUrl) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var adBlockEnabled by remember { mutableStateOf(true) }
    var blockedCount by remember { mutableIntStateOf(0) }
    var desktopMode by remember { mutableStateOf(false) }
    var showStreamsSheet by remember { mutableStateOf(false) }
    var showAdBlockDialog by remember { mutableStateOf(false) }
    val detectedStreams = remember { mutableStateListOf<String>() }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val realUa = remember(desktopMode) { AdBlockFilter.buildRealDeviceUA(context, desktopMode) }
    val antiDetectJs = remember(realUa, desktopMode) {
        buildAntiDetectJs(realUa.chromiumMajor, realUa.fullVersion, desktopMode)
    }

    BackHandler(enabled = canGoBack) { webViewRef?.goBack() }

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

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(backgroundColor)
                    .padding(top = 6.dp)
            ) {
                // Sleek Address Bar Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }

                    Surface(
                        shape = CircleShape,
                        tonalElevation = 3.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                CircleShape,
                            ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = "AdBlock",
                                tint =
                                    if (adBlockEnabled) Color(0xFF4CAF50)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
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
                            BasicTextField(
                                value = addressText,
                                onValueChange = { addressText = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        keyboard?.hide()
                                        val target = normalizeBrowserInput(addressText)
                                        currentUrl = target
                                        detectedStreams.clear()
                                        webViewRef?.loadUrl(target)
                                    }
                                ),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            desktopMode = !desktopMode
                            webViewRef?.let {
                                applyUaAndMetadata(it)
                                it.reload()
                            }
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            if (desktopMode) Icons.Outlined.DesktopWindows else Icons.Outlined.Smartphone,
                            contentDescription = "Desktop/Mobile",
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Reload", modifier = Modifier.size(19.dp))
                    }
                }

                // Quick Bookmarks Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { webViewRef?.goBack() }, enabled = canGoBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { webViewRef?.goForward() }, enabled = canGoForward, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                    SocialPlatform.entries.forEach { platform ->
                        val selected = selectedPlatform == platform
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedPlatform = platform
                                currentUrl = platform.homeUrl
                                addressText = platform.homeUrl
                                detectedStreams.clear()
                                webViewRef?.loadUrl(platform.homeUrl)
                            },
                            shape = CircleShape,
                            label = { Text(platform.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(platform.accentHex).copy(alpha = 0.18f),
                                selectedLabelColor = if (isDarkTheme) Color.White else Color(platform.accentHex),
                            ),
                        )
                    }
                }

                AnimatedVisibility(visible = progress in 1..99, enter = fadeIn(), exit = fadeOut()) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                    )
                }
            }
        },
        containerColor = backgroundColor,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(AndroidColor.BLACK)
                        val cm = CookieManager.getInstance()
                        cm.setAcceptCookie(true)
                        cm.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = LowRamOptimizer.isLowRamDevice(ctx)
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        }
                        applyUaAndMetadata(this)

                        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                            WebViewCompat.addDocumentStartJavaScript(this, antiDetectJs, setOf("*"))
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.deny()
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
                                    currentUrl = url
                                    addressText = url
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
                                    currentUrl = url
                                    addressText = url
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (!url.isNullOrBlank()) {
                                    currentUrl = url
                                    addressText = url
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                view?.evaluateJavascript(antiDetectJs, null)
                                CookieManager.getInstance().flush()
                            }
                        }

                        loadUrl(initialPlatform.homeUrl)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Floating Glassmorphic Bottom Action Bar
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .then(
                        if (isGradientDark) {
                            Modifier.border(
                                1.dp,
                                GradientDarkColors.GlassWhiteBorder,
                                RoundedCornerShape(24.dp),
                            )
                        } else Modifier
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            Downloader.getInfoAndDownload(currentUrl)
                            Toast.makeText(context, "Downloading from page...", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                    ) {
                        Icon(Icons.Outlined.Download, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Download", maxLines = 1)
                    }

                    if (detectedStreams.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { showStreamsSheet = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Icon(Icons.Outlined.VideoFile, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${detectedStreams.size}")
                        }
                    }

                    FilledTonalButton(
                        onClick = { onOpenInPlayer(currentUrl) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                    ) {
                        Icon(Icons.Outlined.PlayCircle, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Watch", maxLines = 1)
                    }

                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
                            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    }
                }
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
                    Text(
                        text = "Blocked $blockedCount requests this session.",
                        style = MaterialTheme.typography.bodySmall,
                    )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Detected Streams (${detectedStreams.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(detectedStreams.toList()) { streamUrl ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = streamUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = {
                                        showStreamsSheet = false
                                        onOpenInPlayer(streamUrl)
                                    }
                                ) {
                                    Icon(Icons.Outlined.PlayCircle, contentDescription = "Play")
                                }
                                IconButton(
                                    onClick = {
                                        Downloader.getInfoAndDownload(streamUrl)
                                        Toast.makeText(context, "Stream download started", Toast.LENGTH_SHORT).show()
                                        showStreamsSheet = false
                                    }
                                ) {
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
