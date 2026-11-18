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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PictureInPictureAlt
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
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
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

private suspend fun resolveStream(rawUrl: String): StreamSource =
    withContext(Dispatchers.IO) {
        val url = rawUrl.trim().let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
        }
        val host = runCatching { java.net.URI(url).host.orEmpty().lowercase() }.getOrDefault("")
        if (
            host.contains("tiktok.com") ||
                host.contains("instagram.com") ||
                host.contains("facebook.com") ||
                host.contains("fb.watch") ||
                host.contains("fb.com")
        ) {
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
            return@withContext StreamSource(
                title = info.title,
                uploader = info.uploader.orEmpty(),
                durationSec = (info.duration ?: 0.0).toLong(),
                thumbnailUrl = info.thumbnail,
                originalUrl = url,
                variants = muxed,
            )
        }

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
        StreamSource(
            title = info.name,
            uploader = info.uploaderName.orEmpty(),
            durationSec = info.duration,
            thumbnailUrl = info.thumbnails.lastOrNull()?.url,
            originalUrl = url,
            variants = list,
        )
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
        views >= 1_000_000_000 -> "%.1fB views".format(views / 1_000_000_000.0)
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
            .onFailure { error = it.message ?: "Search failed" }
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
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 4.dp,
                        modifier =
                            Modifier.fillMaxWidth()
                                .animateContentSize()
                                .then(
                                    if (isGradientDark) {
                                        Modifier.border(
                                            1.dp,
                                            GradientDarkColors.GlassWhiteBorder,
                                            RoundedCornerShape(20.dp),
                                        )
                                    } else Modifier
                                ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (!isAudioMode) {
                                Box(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.Black)
                                ) {
                                    if (!isFullscreen) {
                                        AndroidView(
                                            factory = { ctx ->
                                                PlayerView(ctx).apply {
                                                    this.player = player
                                                    layoutParams = ViewGroup.LayoutParams(-1, -1)
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            FilledTonalIconButton(
                                                onClick = {
                                                    runCatching {
                                                        (context as? Activity)?.enterPictureInPictureMode(
                                                            PictureInPictureParams.Builder().build()
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp),
                                            ) {
                                                Icon(
                                                    Icons.Outlined.PictureInPictureAlt,
                                                    contentDescription = "PiP",
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                        FilledTonalIconButton(
                                            onClick = { isFullscreen = true },
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                            Icon(
                                                Icons.Outlined.Fullscreen,
                                                contentDescription = "Fullscreen",
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
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
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            layoutParams = ViewGroup.LayoutParams(-1, -1)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                FilledTonalIconButton(
                    onClick = { isFullscreen = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                ) {
                    Icon(Icons.Outlined.FullscreenExit, contentDescription = "Exit Fullscreen")
                }
            }
        }
    }
}
