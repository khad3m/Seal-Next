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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

private suspend fun searchYouTube(query: String): List<SearchResultItem> =
    withContext(Dispatchers.IO) {
        ensureNewPipeReady()
        val qh = ServiceList.YouTube.searchQHFactory.fromQuery(query.trim())
        val searchInfo = SearchInfo.getInfo(ServiceList.YouTube, qh)
        searchInfo.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .filter { !it.url.isNullOrBlank() }
            .take(20)
            .map { item ->
                SearchResultItem(
                    title = item.name.orEmpty(),
                    uploader = item.uploaderName.orEmpty(),
                    url = normalizeYouTubeStreamUrl(item.url.orEmpty()),
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
    var selectedVariant by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isAudioMode by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var isLandscapeVideo by remember { mutableStateOf(true) }

    LaunchedEffect(initialUrl) {
        val target = initialUrl?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        input = target
        playRequest = StreamRequest(target, System.nanoTime())
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

    LaunchedEffect(playRequest) {
        val req = playRequest ?: return@LaunchedEffect
        loading = true
        error = null
        playerError = null
        player.stop()
        runCatching { resolveStreamForPlayer(req.value) }
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
        val req = searchRequest ?: return@LaunchedEffect
        loading = true
        error = null
        runCatching { searchYouTube(req.value) }
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
                            if (!isAudioMode) {
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
                            } else {
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
                                            searchRequest = StreamRequest(topic, System.nanoTime())
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
            val activity = context as? Activity
            DisposableEffect(isLandscapeVideo) {
                val window = activity?.window
                val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
                controller?.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller?.hide(WindowInsetsCompat.Type.systemBars())
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
