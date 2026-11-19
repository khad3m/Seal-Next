package com.junkfood.seal.ui.page.stream

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import coil3.compose.AsyncImage
import com.junkfood.seal.Downloader
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.theme.GradientDarkColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import com.junkfood.seal.util.Engines
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private data class SearchResultItem(
    val title: String,
    val uploader: String,
    val url: String,
    val durationSec: Long,
    val viewCount: Long,
    val thumbnailUrl: String?,
)

private data class StreamRequest(val value: String, val nonce: Long)

private fun looksLikeStreamUrl(input: String): Boolean = extractStreamUrl(input) != null

private const val TRENDING_TRIGGER = "__trending__"

private fun mapStreamItem(item: StreamInfoItem): SearchResultItem =
    SearchResultItem(
        title = item.name.orEmpty(),
        uploader = item.uploaderName.orEmpty(),
        url = normalizeYouTubeStreamUrl(item.url.orEmpty()),
        durationSec = item.duration,
        viewCount = item.viewCount,
        thumbnailUrl = item.thumbnails.lastOrNull()?.url,
    )

private suspend fun searchYouTubeNpe(query: String): List<SearchResultItem> =
    withContext(Dispatchers.IO) {
        ensureNewPipeReady()
        val qh = ServiceList.YouTube.searchQHFactory.fromQuery(query.trim())
        val searchInfo = SearchInfo.getInfo(ServiceList.YouTube, qh)
        searchInfo.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .filter { !it.url.isNullOrBlank() }
            .take(20)
            .map(::mapStreamItem)
    }

// Fallback path: NewPipe Extractor can get bot/consent-walled on some networks. yt-dlp's
// `ytsearchN:` pseudo-URL with --flat-playlist returns the same results without a WebView.
private suspend fun searchYouTubeViaYtDlp(query: String): List<SearchResultItem> =
    withContext(Dispatchers.IO) {
        val request =
            YoutubeDLRequest("ytsearch20:${query.trim()}").apply {
                addOption("--flat-playlist")
                addOption("--dump-single-json")
                addOption("--no-warnings")
                addOption("--socket-timeout", "15")
            }
        val response = Engines.yt().execute(request)
        val entries = JSONObject(response.out).optJSONArray("entries") ?: JSONArray()
        (0 until entries.length()).mapNotNull { i ->
            val entry = entries.optJSONObject(i) ?: return@mapNotNull null
            val url = entry.optString("url", "").ifBlank { entry.optString("webpage_url", "") }
            if (url.isBlank()) return@mapNotNull null
            SearchResultItem(
                title = entry.optString("title", url),
                uploader = entry.optString("uploader", "").ifBlank { entry.optString("channel", "") },
                url = normalizeYouTubeStreamUrl(url),
                durationSec = entry.optDouble("duration", 0.0).toLong().coerceAtLeast(0L),
                viewCount = entry.optLong("view_count", 0L).coerceAtLeast(0L),
                thumbnailUrl = thumbnailFromFlatEntry(entry),
            )
        }
    }

private fun thumbnailFromFlatEntry(entry: JSONObject): String? {
    val thumbs = entry.optJSONArray("thumbnails")
    if (thumbs != null) {
        for (i in thumbs.length() - 1 downTo 0) {
            thumbs.optJSONObject(i)?.optString("url", "")?.takeIf { it.startsWith("http") }
                ?.let { return it }
        }
    }
    return entry.optString("thumbnail", "").takeIf { it.startsWith("http") }
}

// NPE first (fast, on-device); on failure or an empty SERP fall back to yt-dlp search.
private suspend fun searchYouTube(query: String): List<SearchResultItem> =
    withContext(Dispatchers.IO) {
        val npeResult = runCatching { searchYouTubeNpe(query) }
        val npeItems = npeResult.getOrNull().orEmpty()
        if (npeItems.isNotEmpty()) return@withContext npeItems
        runCatching { searchYouTubeViaYtDlp(query) }
            .getOrElse { throw npeResult.exceptionOrNull() ?: it }
    }

// Native YouTube feel: the player page's default feed is YouTube's trending kiosk
// (NewPipe Extractor), so the page is useful before a single character is typed.
private suspend fun fetchTrendingYouTube(): List<SearchResultItem> =
    withContext(Dispatchers.IO) {
        ensureNewPipeReady()
        val extractor = ServiceList.YouTube.kioskList.defaultKioskExtractor
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .filter { !it.url.isNullOrBlank() }
            .take(20)
            .map(::mapStreamItem)
    }

private fun formatDuration(sec: Long): String =
    when {
        sec <= 0 -> ""
        sec >= 3600 -> "%d:%02d:%02d".format(sec / 3600, sec % 3600 / 60, sec % 60)
        else -> "%d:%02d".format(sec / 60, sec % 60)
    }

private fun formatViews(views: Long): String =
    when {
        views <= 0 -> ""
        views >= 1_000_000_000 -> "%.1fB views".format(views / 1_000_000.0 / 1000.0)
        views >= 1_000_000 -> "%.1fM views".format(views / 1_000_000.0)
        views >= 1_000 -> "%.1fK views".format(views / 1_000.0)
        else -> "$views views"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamPage(onNavigateBack: () -> Unit, initialUrl: String? = null) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current && isDarkTheme

    var input by rememberSaveable { mutableStateOf(initialUrl.orEmpty()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var source by remember { mutableStateOf<StreamSource?>(null) }
    var searchResults by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var playRequest by remember { mutableStateOf<StreamRequest?>(null) }
    var searchRequest by remember { mutableStateOf<StreamRequest?>(null) }
    var trendingRequest by remember { mutableStateOf<StreamRequest?>(null) }
    var feedLabel by remember { mutableStateOf("Trending now") }
    var selectedVariant by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isAudioMode by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    // bug #3: default to PORTRAIT until the real video size arrives. The old default of
    // `true` forced landscape for vertical videos whenever fullscreen was tapped before
    // the first frame, then flipped to portrait — exactly the reported crash.
    var isLandscapeVideo by remember { mutableStateOf(false) }

    LaunchedEffect(initialUrl) {
        val target = initialUrl?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        input = target
        playRequest = StreamRequest(target, System.nanoTime())
    }

    // Opened with no target link (drawer "YouTube Player") → load the trending feed so
    // the page behaves like the native YouTube app instead of an empty search box.
    LaunchedEffect(Unit) {
        if (initialUrl.isNullOrBlank() && input.isBlank()) {
            trendingRequest = StreamRequest(TRENDING_TRIGGER, System.nanoTime())
        }
    }

    val player = remember { MiniPlayerManager.getOrCreatePlayer(context) }
    DisposableEffect(Unit) {
        val listener =
            object : Player.Listener {
                override fun onPlayerError(e: PlaybackException) {
                    playerError = e.message ?: "Playback failed"
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        isLandscapeVideo = videoSize.width >= videoSize.height
                    }
                }
            }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    fun playVariant(v: StreamVariant, keepPos: Boolean, srcMeta: StreamSource? = source) {
        val pos = if (keepPos) player.currentPosition else PlaybackResumeStore.get(srcMeta?.originalUrl.orEmpty())
        playerError = null
        isAudioMode = v.isAudioOnly
        MiniPlayerManager.playStream(
            context = context,
            streamUrl = v.url,
            title = srcMeta?.title ?: "Streaming",
            uploader = srcMeta?.uploader.orEmpty(),
            thumbnailUrl = srcMeta?.thumbnailUrl,
            sourceUrl = srcMeta?.originalUrl.orEmpty(),
            speed = playbackSpeed,
            positionMs = pos,
            audioUrl = v.audioUrl,
        )
    }

    fun submitInput(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        keyboardController?.hide()
        val extracted = extractStreamUrl(trimmed)
        if (extracted != null) {
            input = extracted
            playRequest = StreamRequest(extracted, System.nanoTime())
        } else {
            searchRequest = StreamRequest(trimmed, System.nanoTime())
        }
    }

    // bug #5: hard 45 s deadline on stream resolution. resolveStreamForPlayer blocks on
    // NewPipe HTTP and on the yt-dlp process; without a timeout a bot-wall or cookie-wall
    // (or a hung yt-dlp) pinned `loading == true` forever and the player never recovered.
    // try/finally keeps `loading` correct even on cancellation.
    LaunchedEffect(playRequest) {
        val req = playRequest ?: return@LaunchedEffect
        loading = true
        error = null
        playerError = null
        searchResults = emptyList()
        player.stop()
        try {
            runCatching { withTimeout(45_000L) { resolveStreamForPlayer(req.value) } }
                .onSuccess {
                    source = it
                    selectedVariant = 0
                    playVariant(it.variants[0], keepPos = false, srcMeta = it)
                }
                .onFailure { th ->
                    source = null
                    if (th !is kotlinx.coroutines.CancellationException) {
                        error = friendlyStreamError(th)
                    }
                }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(searchRequest) {
        val req = searchRequest ?: return@LaunchedEffect
        loading = true
        error = null
        searchResults = emptyList()
        try {
            runCatching { withTimeout(45_000L) { searchYouTube(req.value) } }
                .onSuccess { results ->
                    // bug #6: an empty result (bot/consent walls often return an empty
                    // SERP) used to be silent — now it shows a clear message instead of
                    // looking like the search did nothing.
                    if (results.isEmpty()) {
                        error =
                            "No results found — YouTube may be rate-limiting this network. " +
                                "Try different wording or paste a direct link."
                    } else {
                        searchResults = results
                        feedLabel = "Search results"
                    }
                }
                .onFailure { th ->
                    if (th !is kotlinx.coroutines.CancellationException) {
                        error = friendlyStreamError(th)
                    }
                }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(trendingRequest) {
        val req = trendingRequest ?: return@LaunchedEffect
        if (req.value != TRENDING_TRIGGER) return@LaunchedEffect
        loading = true
        error = null
        try {
            runCatching { withTimeout(45_000L) { fetchTrendingYouTube() } }
                .onSuccess { results ->
                    // A search typed while trending was loading wins — never overwrite it.
                    if (results.isNotEmpty() && searchResults.isEmpty()) {
                        searchResults = results
                        feedLabel = "Trending now"
                    }
                }
                .onFailure { th ->
                    // Soft-fail: trending is a bonus feed; it must never block manual use.
                    if (th !is kotlinx.coroutines.CancellationException && searchResults.isEmpty()) {
                        error = friendlyStreamError(th)
                    }
                }
        } finally {
            loading = false
        }
    }

    val backgroundColor =
        if (isGradientDark) GradientDarkColors.Background else MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "YouTube & Stream Player",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Native YouTube — search & trending (no WebView for YouTube)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = { BackButton(onNavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
            )
        },
        containerColor = backgroundColor,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Search YouTube or paste video URL...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (input.isNotEmpty()) {
                                IconButton(onClick = { input = "" }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                                }
                            }
                            IconButton(
                                onClick = {
                                    clipboard.getText()?.text?.let { pasted ->
                                        val clean = extractStreamUrl(pasted) ?: pasted.trim()
                                        input = clean
                                        if (looksLikeStreamUrl(clean)) submitInput(clean)
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submitInput(input) }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { submitInput(input) },
                        enabled = !loading && input.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(
                            imageVector =
                                if (looksLikeStreamUrl(input)) Icons.Outlined.PlayCircle
                                else Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                loading -> "Loading..."
                                looksLikeStreamUrl(input) -> "Play Stream"
                                else -> "Search YouTube"
                            }
                        )
                    }
                }
            }

            if (loading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            error?.let { errMsg ->
                item {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            text = errMsg,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            source?.let { src ->
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        tonalElevation = 4.dp,
                        modifier =
                            Modifier.fillMaxWidth()
                                .animateContentSize()
                                .then(
                                    if (isGradientDark) {
                                        Modifier.border(
                                            1.dp,
                                            GradientDarkColors.GlassWhiteBorder,
                                            RoundedCornerShape(22.dp),
                                        )
                                    } else Modifier
                                ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // bug #3: render the inline PlayerView only when NOT in
                            // fullscreen. Previously both the inline view and the fullscreen
                            // Dialog attached PlayerViews to the same ExoPlayer — the dialog
                            // stole the video surface and dismissal left it pointing at a
                            // destroyed window surface, crashing on codec re-init.
                            if (!isAudioMode && !isFullscreen) {
                                GlassPlayerSurface(
                                    player = player,
                                    title = src.title,
                                    uploader = src.uploader,
                                    isFullscreen = false,
                                    variants = src.variants,
                                    selectedVariant = selectedVariant,
                                    sourceUrl = src.originalUrl,
                                    thumbnailUrl = src.thumbnailUrl,
                                    onSelectVariant = { i ->
                                        selectedVariant = i
                                        playVariant(src.variants[i], keepPos = true)
                                    },
                                    onEnterFullscreen = { isFullscreen = true },
                                )
                            } else if (isAudioMode) {
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                            .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(68.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (!src.thumbnailUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = src.thumbnailUrl,
                                                contentDescription = src.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            Icon(
                                                Icons.Outlined.Headphones,
                                                contentDescription = null,
                                                modifier = Modifier.size(30.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        }
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Audio-only playback",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            src.uploader.ifBlank { "Audio stream" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    FilledTonalIconButton(onClick = { MiniPlayerManager.togglePlayPause() }) {
                                        Icon(Icons.Outlined.PlayCircle, contentDescription = "Play / Pause")
                                    }
                                }
                            }

                            playerError?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Playback error: $it",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = src.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text =
                                    listOf(src.uploader, formatDuration(src.durationSec))
                                        .filter { it.isNotBlank() }
                                        .joinToString("  •  "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                src.variants.forEachIndexed { i, v ->
                                    FilterChip(
                                        selected = i == selectedVariant,
                                        onClick = {
                                            selectedVariant = i
                                            playVariant(v, keepPos = true)
                                        },
                                        label = { Text(if (v.meta.isNotBlank()) "${v.label} · ${v.meta}" else v.label) },
                                    )
                                }
                            }

                            Row(
                                modifier =
                                    Modifier.horizontalScroll(rememberScrollState())
                                        .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.Speed,
                                    contentDescription = "Speed",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                    FilterChip(
                                        selected = playbackSpeed == spd,
                                        onClick = {
                                            playbackSpeed = spd
                                            player.setPlaybackSpeed(spd)
                                        },
                                        label = { Text("${spd}x") },
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        Downloader.getInfoAndDownload(src.originalUrl)
                                        Toast.makeText(
                                                context,
                                                "Download started with Seal Next",
                                                Toast.LENGTH_SHORT,
                                            )
                                            .show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Download")
                                }
                                FilledTonalIconButton(
                                    onClick = {
                                        val cm =
                                            context.getSystemService(Context.CLIPBOARD_SERVICE)
                                                as ClipboardManager
                                        cm.setPrimaryClip(
                                            ClipData.newPlainText("Stream URL", src.originalUrl)
                                        )
                                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
                                }
                            }
                        }
                    }
                }
            }

            // bug #6: the "Quick Discover" hardcoded suggestion chips were removed
            // entirely per user request.

            if (searchResults.isNotEmpty()) {
                item {
                        Text(
                            text = feedLabel,
                            style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                itemsIndexed(searchResults, key = { index, item -> "${index}_${item.url}" }) { _, item ->
                    ElevatedCard(
                        onClick = {
                            input = item.url
                            playRequest = StreamRequest(item.url, System.nanoTime())
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(width = 124.dp, height = 70.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                val dur = formatDuration(item.durationSec)
                                if (dur.isNotEmpty()) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.78f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                                    ) {
                                        Text(
                                            text = dur,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text =
                                        listOf(item.uploader, formatViews(item.viewCount))
                                            .filter { it.isNotBlank() }
                                            .joinToString("  •  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                ),
        ) {
            // bug #3: ONE orientation decision per fullscreen session. The old effect was
            // keyed on isLandscapeVideo, so every aspect-ratio change (HLS variant switch,
            // embedded ad) re-fired it and thrashed requestedOrientation mid-playback —
            // codec/surface re-init + window resize at the same time = the crash window.
            // Also: hide system bars on the DIALOG window (the player lives in the Dialog's
            // own window; hiding bars on the activity window did nothing in fullscreen).
            val dialogWindow =
                (LocalView.current.parent as? DialogWindowProvider)?.window
                    ?: (context as? Activity)?.window
            DisposableEffect(Unit) {
                val controller =
                    dialogWindow?.let { WindowCompat.getInsetsController(it, it.decorView) }
                controller?.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller?.hide(WindowInsetsCompat.Type.systemBars())
                val activity = context as? Activity
                activity?.requestedOrientation =
                    if (isLandscapeVideo) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                onDispose {
                    controller?.show(WindowInsetsCompat.Type.systemBars())
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                source?.let { src ->
                    GlassPlayerSurface(
                        player = player,
                        title = src.title,
                        uploader = src.uploader,
                        isFullscreen = true,
                        variants = src.variants,
                        selectedVariant = selectedVariant,
                        sourceUrl = src.originalUrl,
                                    thumbnailUrl = src.thumbnailUrl,
                        onSelectVariant = { i ->
                            selectedVariant = i
                            playVariant(src.variants[i], keepPos = true)
                        },
                        onExitFullscreen = { isFullscreen = false },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
