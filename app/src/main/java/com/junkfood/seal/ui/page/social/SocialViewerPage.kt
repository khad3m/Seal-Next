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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.Expand
import androidx.compose.material.icons.outlined.Link
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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

    // PWA-style home: false shows the native launcher with platform cards; the WebView
    // only becomes visible after the user picks a site (bug #3: the browser UI used to
    // appear immediately with an address bar and action buttons).
    var browsing by remember { mutableStateOf(false) }
    // Floating action cluster (bug #3): one mini circular button that expands into the
    // Download / Watch / Copy / Streams actions instead of an always-visible bar.
    var actionsExpanded by remember { mutableStateOf(false) }
    var manualAddress by remember { mutableStateOf("") }

    val realUa = remember(desktopMode) { AdBlockFilter.buildRealDeviceUA(context, desktopMode) }
    val antiDetectJs = remember(realUa, desktopMode) {
        buildAntiDetectJs(realUa.chromiumMajor, realUa.fullVersion, desktopMode)
    }

    BackHandler(enabled = browsing || canGoBack) {
        when {
            browsing && webViewRef?.canGoBack() == true -> webViewRef?.goBack()
            browsing && currentUrl != selectedPlatform.homeUrl -> {
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

    fun launchPlatform(platform: SocialPlatform) {
        selectedPlatform = platform
        currentUrl = platform.homeUrl
        addressText = platform.homeUrl
        detectedStreams.clear()
        browsing = true
        actionsExpanded = false
        webViewRef?.loadUrl(platform.homeUrl)
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        // ---------- WebView layer (kept composed so back/forward survive, hidden on home) ----------
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

                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ---------- Browser chrome overlay (only while browsing) ----------
        AnimatedVisibility(
            visible = browsing,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(backgroundColor)
                        .statusBarsPadding()
                        .padding(top = 6.dp),
            ) {
                // Address bar row
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = {
                            // Close chrome → back to the PWA-style home screen
                            browsing = false
                            webViewRef?.stopLoading()
                        },
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close browser")
                    }

                    Surface(
                        shape = CircleShape,
                        tonalElevation = 3.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier =
                            Modifier.weight(1f)
                                .height(42.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    CircleShape,
                                ),
                    ) {
                        Row(
                            modifier =
                                Modifier.fillMaxSize()
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
                            BasicTextField(
                                value = addressText,
                                onValueChange = { addressText = it },
                                singleLine = true,
                                textStyle =
                                    MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions =
                                    KeyboardActions(
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

                // Back / forward + platform quick switch strip
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
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
                        FilterChip(
                            selected = selectedPlatform == platform,
                            onClick = { launchPlatform(platform) },
                            shape = CircleShape,
                            label = { Text(platform.label, style = MaterialTheme.typography.labelMedium) },
                            colors =
                                FilterChipDefaults.filterChipColors(
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
        }

        // ---------- PWA-style native home (bug #3) ----------
        AnimatedVisibility(
            visible = !browsing,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            BrowserHomeScreen(
                isGradientDark = isGradientDark,
                manualAddress = manualAddress,
                onManualAddressChange = { manualAddress = it },
                onOpenPlatform = { launchPlatform(it) },
                onOpenAddress = { raw ->
                    keyboard?.hide()
                    val target = normalizeBrowserInput(raw)
                    selectedPlatform = SocialPlatform.GOOGLE
                    currentUrl = target
                    addressText = target
                    detectedStreams.clear()
                    browsing = true
                    actionsExpanded = false
                    webViewRef?.loadUrl(target)
                },
                onNavigateBack = onNavigateBack,
            )
        }

        // ---------- Floating expandable actions (bug #3) ----------
        AnimatedVisibility(
            visible = browsing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 14.dp),
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimatedVisibility(
                    visible = actionsExpanded,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.62f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FloatingGlassAction(
                                icon = Icons.Outlined.Download,
                                label = "Download",
                            ) {
                                actionsExpanded = false
                                Downloader.getInfoAndDownload(currentUrl)
                                Toast.makeText(context, "Downloading from page...", Toast.LENGTH_SHORT).show()
                            }
                            if (detectedStreams.isNotEmpty()) {
                                FloatingGlassAction(
                                    icon = Icons.Outlined.VideoFile,
                                    label = "Streams (${detectedStreams.size})",
                                ) {
                                    actionsExpanded = false
                                    showStreamsSheet = true
                                }
                            }
                            FloatingGlassAction(
                                icon = Icons.Outlined.PlayCircle,
                                label = "Watch",
                            ) {
                                actionsExpanded = false
                                onOpenInPlayer(currentUrl)
                            }
                            FloatingGlassAction(
                                icon = Icons.Outlined.ContentCopy,
                                label = "Copy link",
                            ) {
                                actionsExpanded = false
                                val cm =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
                                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // The single mini circular button
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val fabScale by animateFloatAsState(
                    targetValue = if (pressed) 0.92f else 1f,
                    label = "fab_scale",
                )
                Surface(
                    onClick = { actionsExpanded = !actionsExpanded },
                    interactionSource = interaction,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(54.dp).scale(fabScale),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Expand,
                            contentDescription = if (actionsExpanded) "Collapse actions" else "Expand actions",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                        )
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
                modifier =
                    Modifier.fillMaxWidth()
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

/** PWA-style launcher shown instead of the always-on browser UI (bug #3). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserHomeScreen(
    isGradientDark: Boolean,
    manualAddress: String,
    onManualAddressChange: (String) -> Unit,
    onOpenPlatform: (SocialPlatform) -> Unit,
    onOpenAddress: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(
                    if (isGradientDark) {
                        Brush.verticalGradient(
                            listOf(
                                GradientDarkColors.Background,
                                Color.Black,
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                            )
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
            Column {
                Text(
                    text = "Seal Next Browser",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Open your favourite apps like native screens",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Platform cards — PWA-style native launchers
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SocialPlatform.entries.filter { it != SocialPlatform.GOOGLE }.chunked(2).forEach { rowPlatforms ->
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

        // Manual address card
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
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Open any link",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                OutlinedTextField(
                    value = manualAddress,
                    onValueChange = onManualAddressChange,
                    placeholder = { Text("Type an address or search...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions =
                        KeyboardActions(
                            onGo = { if (manualAddress.isNotBlank()) onOpenAddress(manualAddress) }
                        ),
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

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Back to Seal Next")
        }
        Spacer(Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
            Surface(
                shape = CircleShape,
                color = Color(platform.accentHex).copy(alpha = 0.16f),
            ) {
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

@Composable
private fun FloatingGlassAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
