package com.junkfood.seal.ui.page.stream

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.junkfood.seal.Downloader
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.LowRamOptimizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private data class Variant(val label: String, val url: String, val isAudioOnly: Boolean = false)

private data class StreamSource(
    val title: String,
    val uploader: String,
    val durationSec: Long,
    val thumbnailUrl: String?,
    val originalUrl: String,
    val variants: List<Variant>,
)

private data class SearchResultItem(
    val title: String,
    val uploader: String,
    val url: String,
    val durationSec: Long,
    val viewCount: Long,
    val thumbnailUrl: String?,
)

@Volatile
private var npInit = false

private fun ensureNewPipeInit() {
    if (!npInit) {
        synchronized(NewPipeDownloader) {
            if (!npInit) {
                NewPipe.init(NewPipeDownloader)
                npInit = true
            }
        }
    }
}

private fun looksLikeUrl(input: String): Boolean {
    val t = input.trim().lowercase()
    return t.startsWith("http://") ||
        t.startsWith("https://") ||
        t.contains("youtube.com/") ||
        t.contains("youtu.be/") ||
        t.contains("tiktok.com/") ||
        t.contains("instagram.com/") ||
        t.contains("facebook.com/") ||
        t.contains("fb.watch/")
}

/** Maps raw extraction failures to short, actionable messages instead of stack-trace noise. */
private fun friendlyStreamError(th: Throwable?): String {
    val msg = th?.message.orEmpty()
    return when {
        msg.contains("Sign in to confirm", ignoreCase = true) ||
            msg.contains("bot", ignoreCase = true) ->
            "YouTube flagged this request as a bot. Play another video, or tap Download — " +
                "the downloader uses a different engine."

        msg.contains("nsig", ignoreCase = true) ||
            msg.contains("JavaScript runtime", ignoreCase = true) ->
            "YouTube changed its playback protection. Seal Next auto-updates yt-dlp in the " +
                "background — retry in a few minutes, or tap Download instead."

        msg.contains("unavailable", ignoreCase = true) ||
            msg.contains("removed", ignoreCase = true) ->
            "This video is unavailable — it may be removed, private or region-locked."

        msg.contains("HTTP", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
            "Network error while contacting the video service. Check your connection and retry."

        msg.isNotBlank() -> msg
        else -> "Could not load this stream. Try again, or tap Download."
    }
}

/** Extracts muxed (video+audio) variants through the yt-dlp engine. Used for social hosts
 *  and as the fallback engine whenever NewPipeExtractor cannot resolve a YouTube URL. */
private suspend fun resolveViaYtDlp(url: String): StreamSource {
    val info = DownloadUtil.fetchVideoInfoFromUrl(url).getOrThrow()
    val muxed =
        info.formats
            .orEmpty()
            .filter {
                !it.url.isNullOrBlank() &&
                    it.vcodec != "none" &&
                    it.acodec != "none" &&
                    it.url!!.startsWith("http") &&
                    it.ext != "m3u8"
            }
            .sortedByDescending { it.height ?: 0.0 }
            .distinctBy { it.height?.toInt() ?: 0 }
            .map {
                Variant(
                    label = it.height?.let { h -> "${h.toInt()}p" } ?: "Auto",
                    url = it.url!!,
                )
            }
    if (muxed.isEmpty()) error("No directly playable stream found (cookies/login may be required)")
    return StreamSource(
        title = info.title,
        uploader = info.uploader.orEmpty(),
        durationSec = (info.duration ?: 0.0).toLong(),
        thumbnailUrl = info.thumbnail,
        originalUrl = url,
        variants = muxed,
    )
}

/** Resolves a YouTube URL through NewPipeExtractor into playable variants. */
private suspend fun resolveViaNewPipe(url: String): StreamSource {
    ensureNewPipeInit()
    val info = StreamInfo.getInfo(ServiceList.YouTube, url)
    val list =
        info.videoStreams
            .filter { !it.isVideoOnly && it.content.isNotBlank() }
            .sortedByDescending { it.height }
            .distinctBy { it.height }
            .map { Variant(if (it.height > 0) "${it.height}p" else "Auto", it.content) }
            .toMutableList()
    if (!info.hlsUrl.isNullOrBlank()) {
        list.add(Variant("Auto (HLS)", info.hlsUrl))
    }
    info.audioStreams
        .filter { it.content.isNotBlank() }
        .maxByOrNull { it.averageBitrate }
        ?.let { audio ->
            list.add(Variant("Audio Only", audio.content, isAudioOnly = true))
        }
    if (list.isEmpty()) error("No playable stream found for this URL")
    return StreamSource(
        title = info.name,
        uploader = info.uploaderName.orEmpty(),
        durationSec = info.duration,
        thumbnailUrl = info.thumbnails.lastOrNull()?.url,
        originalUrl = url,
        variants = list,
    )
}

private suspend fun resolveStream(rawUrl: String): StreamSource =
    withContext(Dispatchers.IO) {
        val url = rawUrl.trim().let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
        }
        val host = runCatching { java.net.URI(url).host.orEmpty().lowercase() }.getOrDefault("")

        when {
            // Social hosts: yt-dlp is the only reliable engine (NewPipe has no extractors
            // for TikTok/Instagram/Facebook playback URLs).
            host.contains("tiktok.com") ||
                host.contains("instagram.com") ||
                host.contains("facebook.com") ||
                host.contains("fb.watch") ||
                host.contains("fb.com") ->
                runCatching { resolveViaYtDlp(url) }
                    .getOrElse { throw Exception(friendlyStreamError(it)) }

            // YouTube (and everything else): try NewPipeExtractor first, then fall back to
            // the yt-dlp engine. A single failed extractor no longer kills playback — this
            // was the source of most "error: ... missing ..." reports on YouTube links.
            else -> {
                val npResult = runCatching { resolveViaNewPipe(url) }
                npResult.getOrNull()
                    ?: runCatching { resolveViaYtDlp(url) }
                        .getOrElse {
                            throw Exception(friendlyStreamError(npResult.exceptionOrNull() ?: it))
                        }
            }
        }
    }

private suspend fun searchYouTube(query: String): List<SearchResultItem> =
    withContext(Dispatchers.IO) {
        ensureNewPipeInit()
        val qh = ServiceList.YouTube.searchQHFactory.fromQuery(query.trim())
        val searchInfo = SearchInfo.getInfo(ServiceList.YouTube, qh)
        searchInfo.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .take(20)
            .map { item ->
                SearchResultItem(
                    title = item.name.orEmpty(),
                    uploader = item.uploaderName.orEmpty(),
                    url = item.url.orEmpty(),
                    durationSec = item.duration,
                    viewCount = item.viewCount,
                    thumbnailUrl = item.thumbnails.lastOrNull()?.url,
                )
            }
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

/** Live snapshot of the player used by the glass control overlay. */
private data class PlayerUi(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
)

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
    var playRequest by remember { mutableStateOf<String?>(initialUrl?.takeIf { it.isNotBlank() }) }
    var searchRequest by remember { mutableStateOf<String?>(null) }
    var selectedVariant by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isAudioMode by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }

    val player = remember { MiniPlayerManager.getOrCreatePlayer(context) }
    DisposableEffect(Unit) {
        val listener =
            object : Player.Listener {
                override fun onPlayerError(e: PlaybackException) {
                    playerError = e.message ?: "Playback failed"
                }
            }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    fun playVariant(v: Variant, keepPos: Boolean, srcMeta: StreamSource? = source) {
        val pos = if (keepPos) player.currentPosition else 0L
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
        )
    }

    fun submitInput(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        keyboardController?.hide()
        if (looksLikeUrl(trimmed)) {
            playRequest = trimmed
        } else {
            searchRequest = trimmed
        }
    }

    LaunchedEffect(playRequest) {
        val url = playRequest ?: return@LaunchedEffect
        loading = true
        error = null
        playerError = null
        player.stop()
        runCatching { resolveStream(url) }
            .onSuccess {
                source = it
                selectedVariant = 0
                playVariant(it.variants[0], keepPos = false, srcMeta = it)
            }
            .onFailure {
                source = null
                error = it.message ?: "Could not load stream"
            }
        loading = false
    }

    LaunchedEffect(searchRequest) {
        val q = searchRequest ?: return@LaunchedEffect
        loading = true
        error = null
        runCatching { searchYouTube(q) }
            .onSuccess { searchResults = it }
            .onFailure { error = friendlyStreamError(it) }
        loading = false
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
                            text = "Search YouTube or paste YT / IG / TikTok / FB link",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = { BackButton(onNavigateBack) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor
                    ),
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
                                    clipboard.getText()?.text?.let {
                                        input = it
                                        if (looksLikeUrl(it)) submitInput(it)
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
                                if (looksLikeUrl(input)) Icons.Outlined.PlayCircle
                                else Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                loading -> "Loading..."
                                looksLikeUrl(input) -> "Play Stream"
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
                            if (!isAudioMode) {
                                GlassPlayerSurface(
                                    player = player,
                                    title = src.title,
                                    uploader = src.uploader,
                                    thumbnailUrl = src.thumbnailUrl,
                                    isFullscreen = false,
                                    onEnterFullscreen = { isFullscreen = true },
                                )
                            } else {
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        MaterialTheme.colorScheme.secondaryContainer,
                                                    )
                                                )
                                            )
                                            .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.Headphones,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Audio-Only Mode (Low RAM & Battery)",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            "Streaming audio without decoding video frames",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
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
                            // Quality / Audio chips
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
                                        label = { Text(v.label) },
                                    )
                                }
                            }

                            // Playback speed chips
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

            if (source == null && searchResults.isEmpty() && !loading) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Quick Discover",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("Lofi Beats", "4K Nature", "Tech Review", "Islamic Nasheed", "Podcast")
                                .forEach { topic ->
                                    SuggestionChip(
                                        onClick = {
                                            input = topic
                                            searchRequest = topic
                                        },
                                        label = { Text(topic) },
                                    )
                                }
                        }
                    }
                }
            }

            if (searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Search Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(searchResults, key = { it.url }) { item ->
                    ElevatedCard(
                        onClick = {
                            input = item.url
                            playRequest = item.url
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
                                        modifier =
                                            Modifier.align(Alignment.BottomEnd).padding(4.dp),
                                    ) {
                                        Text(
                                            text = dur,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier =
                                                Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
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
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val activity = context as? Activity
            DisposableEffect(Unit) {
                val window = activity?.window
                val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
                controller?.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller?.hide(WindowInsetsCompat.Type.systemBars())
                onDispose {
                    controller?.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                source?.let { src ->
                    GlassPlayerSurface(
                        player = player,
                        title = src.title,
                        uploader = src.uploader,
                        thumbnailUrl = src.thumbnailUrl,
                        isFullscreen = true,
                        onExitFullscreen = { isFullscreen = false },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/** Formats milliseconds as m:ss / h:mm:ss for the player time labels. */
private fun formatPlayerTime(ms: Long): String {
    val totalSec = ms / 1000
    return when {
        totalSec >= 3600 -> "%d:%02d:%02d".format(totalSec / 3600, totalSec % 3600 / 60, totalSec % 60)
        else -> "%d:%02d".format(totalSec / 60, totalSec % 60)
    }
}

private enum class GestureFeedback { None, Seek, Volume, Brightness }

/**
 * Premium glassmorphism player surface with full gesture support:
 *
 *  • Tap            — show / hide the glass control overlay
 *  • Double-tap     — left third rewinds 10s, right third forwards 10s
 *  • Horizontal drag — scrub through the video with a live glass position bubble
 *  • Vertical drag on the LEFT half  — screen brightness
 *  • Vertical drag on the RIGHT half — player volume
 *
 * Controls auto-hide after a few seconds while playing. All chrome is translucent
 * frosted glass (blurred thumbnail backdrop on Android 12+, gradient scrim below).
 */
@Composable
private fun GlassPlayerSurface(
    player: Player,
    title: String,
    uploader: String,
    thumbnailUrl: String?,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier,
    onEnterFullscreen: () -> Unit = {},
    onExitFullscreen: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val ui by produceState(PlayerUi(), player) {
        while (true) {
            value =
                PlayerUi(
                    isPlaying = player.isPlaying,
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.coerceAtLeast(0L),
                    bufferedMs = player.bufferedPosition.coerceAtLeast(0L),
                )
            delay(300)
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var gestureFeedback by remember { mutableStateOf(GestureFeedback.None) }
    var feedbackValue by remember { mutableFloatStateOf(0f) }
    var scrubTargetMs by remember { mutableLongStateOf(-1L) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }

    // Auto-hide the overlay a few seconds after it appears while playback is running.
    LaunchedEffect(controlsVisible, ui.isPlaying) {
        if (controlsVisible && ui.isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    Box(
        modifier =
            (if (isFullscreen) modifier else modifier.fillMaxWidth().aspectRatio(16f / 9f))
                .clip(RoundedCornerShape(if (isFullscreen) 0.dp else 16.dp))
                .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    layoutParams = ViewGroup.LayoutParams(-1, -1)
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Scrim behind the overlay so glass elements stay readable on bright scenes.
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.55f),
                                0.35f to Color.Transparent,
                                0.7f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.72f),
                            )
                        )
            )
        }

        // Gesture layer — sits above the video, below the controls.
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                val third = size.width / 3f
                                val delta =
                                    if (offset.x < third) -10_000L else if (offset.x > third * 2) 10_000L else 0L
                                if (delta != 0L) {
                                    val target =
                                        (player.currentPosition + delta)
                                            .coerceIn(0L, player.duration.coerceAtLeast(0L))
                                    player.seekTo(target)
                                    scrubTargetMs = target
                                    gestureFeedback = GestureFeedback.Seek
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { if (ui.durationMs > 0) scrubTargetMs = ui.positionMs },
                            onDragEnd = {
                                if (scrubTargetMs >= 0) player.seekTo(scrubTargetMs)
                                scrubTargetMs = -1L
                                gestureFeedback = GestureFeedback.None
                            },
                            onDragCancel = { scrubTargetMs = -1L },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                if (ui.durationMs > 0) {
                                    val step = dragAmount / size.width * ui.durationMs
                                    scrubTargetMs =
                                        ((if (scrubTargetMs < 0) ui.positionMs else scrubTargetMs) + step.toLong())
                                            .coerceIn(0L, ui.durationMs)
                                    gestureFeedback = GestureFeedback.Seek
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val step = -dragAmount / size.height
                                when {
                                    change.position.x < size.width / 2f -> {
                                        val window = activity?.window ?: return@detectVerticalDragGestures
                                        val attrs = window.attributes
                                        val current =
                                            if (attrs.screenBrightness < 0f)
                                                0.5f
                                            else attrs.screenBrightness
                                        val next = (current + step).coerceIn(0.01f, 1f)
                                        attrs.screenBrightness = next
                                        window.attributes = attrs
                                        feedbackValue = next
                                        gestureFeedback = GestureFeedback.Brightness
                                    }
                                    else -> {
                                        val next = (player.volume + step).coerceIn(0f, 1f)
                                        player.volume = next
                                        feedbackValue = next
                                        gestureFeedback = GestureFeedback.Volume
                                    }
                                }
                            },
                            onDragEnd = { gestureFeedback = GestureFeedback.None },
                            onDragCancel = { gestureFeedback = GestureFeedback.None },
                        )
                    }
        )

        // Glass feedback bubble for double-tap seek / scrubbing / volume / brightness.
        AnimatedVisibility(
            visible = gestureFeedback != GestureFeedback.None,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border =
                    BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.25f),
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val icon =
                        when (gestureFeedback) {
                            GestureFeedback.Volume -> Icons.Outlined.VolumeUp
                            GestureFeedback.Brightness -> Icons.Outlined.WbSunny
                            else -> Icons.Outlined.Forward10
                        }
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text =
                            when (gestureFeedback) {
                                GestureFeedback.Seek ->
                                    formatPlayerTime(if (scrubTargetMs >= 0) scrubTargetMs else ui.positionMs)
                                GestureFeedback.Volume -> "${(feedbackValue * 100).toInt()}%"
                                GestureFeedback.Brightness -> "${(feedbackValue * 100).toInt()}%"
                                GestureFeedback.None -> ""
                            },
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Top overlay: title chip + PiP / fullscreen.
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = title,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (uploader.isNotBlank()) {
                            Text(
                                text = uploader,
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                GlassControlButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        runCatching { activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().build()) }
                    }
                }) {
                    Icon(
                        Icons.Outlined.PictureInPictureAlt,
                        contentDescription = "Picture in picture",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                GlassControlButton(onClick = {
                    if (isFullscreen) onExitFullscreen() else onEnterFullscreen()
                }) {
                    Icon(
                        if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Center overlay: ±10s + play/pause.
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassControlButton(size = 52.dp, onClick = {
                    player.seekTo(
                        (player.currentPosition - 10_000L).coerceIn(0L, player.duration.coerceAtLeast(0L))
                    )
                }) {
                    Icon(Icons.Outlined.Replay10, "Rewind 10 seconds", tint = Color.White, modifier = Modifier.size(26.dp))
                }
                GlassControlButton(size = 68.dp, onClick = { MiniPlayerManager.togglePlayPause() }) {
                    Icon(
                        if (ui.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (ui.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }
                GlassControlButton(size = 52.dp, onClick = {
                    player.seekTo(
                        (player.currentPosition + 10_000L).coerceIn(0L, player.duration.coerceAtLeast(0L))
                    )
                }) {
                    Icon(Icons.Outlined.Forward10, "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }

        // Bottom overlay: glass seekbar + time + speed / quality triggers.
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Slider(
                    value =
                        when {
                            scrubTargetMs >= 0 -> scrubTargetMs.toFloat()
                            ui.durationMs > 0 -> ui.positionMs.toFloat()
                            else -> 0f
                        },
                    onValueChange = {
                        scrubTargetMs = it.toLong()
                        gestureFeedback = GestureFeedback.Seek
                    },
                    onValueChangeFinished = {
                        if (scrubTargetMs >= 0) player.seekTo(scrubTargetMs)
                        scrubTargetMs = -1L
                        gestureFeedback = GestureFeedback.None
                    },
                    valueRange = 0f..(if (ui.durationMs > 0) ui.durationMs.toFloat() else 1f),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                        ),
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatPlayerTime(if (scrubTargetMs >= 0) scrubTargetMs else ui.positionMs),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = "  /  ${formatPlayerTime(ui.durationMs)}",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.weight(1f))
                    GlassControlButton(onClick = { showSpeedSheet = true }) {
                        Icon(Icons.Outlined.Speed, "Playback speed", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    if (isFullscreen) {
                        Spacer(Modifier.width(6.dp))
                        GlassControlButton(onClick = { showQualitySheet = true }) {
                            Icon(Icons.Outlined.HighQuality, "Quality", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Fullscreen keeps its own quality sheet so the overlay works in both modes.
    if (showQualitySheet) {
        Dialog(onDismissRequest = { showQualitySheet = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.8f),
            ) {
                Text(
                    text = "Use the quality chips below the player (or exit fullscreen)",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(22.dp),
                )
            }
        }
    }
    if (showSpeedSheet) {
        SpeedPickerDialog(
            current = (player.playbackParameters.speed * 100).toInt() / 100f,
            onSelect = { player.setPlaybackSpeed(it) },
            onDismiss = { showSpeedSheet = false },
        )
    }
}

@Composable
private fun GlassControlButton(
    size: androidx.compose.ui.unit.Dp = 38.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun SpeedPickerDialog(
    current: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Playback speed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                        FilterChip(
                            selected = current == spd,
                            onClick = {
                                onSelect(spd)
                                onDismiss()
                            },
                            label = { Text("${spd}x") },
                        )
                    }
                }
            }
        }
    }
}
