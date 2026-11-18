package com.junkfood.seal.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.Toast
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import com.junkfood.seal.ui.page.stream.GlassPlayerSurface
import com.junkfood.seal.ui.page.stream.MiniPlayerManager
import com.junkfood.seal.ui.page.stream.StreamSource
import com.junkfood.seal.ui.page.stream.resolveStreamForPlayer
import com.junkfood.seal.ui.page.stream.streamSourceFromVideoInfo
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.VideoInfo
import com.junkfood.seal.util.PlaylistResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Premium fetch sheet shown when a link is submitted from the home screen with
 * "Configure before download" enabled.
 *
 * The sheet automatically fetches the media info, then presents:
 *  • a rich preview card (thumbnail backdrop, title, uploader, duration)
 *  • download-as preset chips (video quality presets + audio presets)
 *  • a grid of the actual qualities available for THIS video, mined from the
 *    fetched formats (so the user picks real heights, not blind guesses)
 *  • one Download button that maps the selection onto [DownloadUtil.DownloadPreferences]
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FetchPreviewSheet(
    url: String,
    onDismiss: () -> Unit,
    onDownload: (DownloadUtil.DownloadPreferences) -> Unit,
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current && isDarkTheme
    val platform = remember(url) { detectPlatformFromUrl(url) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var fetching by remember(url) { mutableStateOf(true) }
    var videoInfo by remember(url) { mutableStateOf<VideoInfo?>(null) }
    var playlistInfo by remember(url) { mutableStateOf<PlaylistResult?>(null) }
    var errorMessage by remember(url) { mutableStateOf<String?>(null) }
    var retryTrigger by remember(url) { mutableStateOf(0) }

    // Selection state: -1 = no explicit selection (use the saved preset); >= 0 = video
    // resolution code from PreferenceUtil (1=2160p ... 6=360p); audio presets carry a
    // separate code in audioPreset (0 = best, 1..3 = quality ladder).
    var selectedVideoPreset by remember(url) { mutableIntStateOf(-1) }
    var selectedAudioPreset by remember(url) { mutableIntStateOf(-1) }
    var audioOnly by remember(url) { mutableStateOf(false) }
    var directStreamSource by remember(url) { mutableStateOf<StreamSource?>(null) }
    var showDirectPlayer by remember(url) { mutableStateOf(false) }
    var resolvingStream by remember(url) { mutableStateOf(false) }
    var triggerDirectStream by remember(url) { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(triggerDirectStream) {
        if (triggerDirectStream == 0) return@LaunchedEffect
        resolvingStream = true
        val localSource = videoInfo?.let { streamSourceFromVideoInfo(it, url) }
        val resolved = localSource ?: runCatching { resolveStreamForPlayer(url) }.getOrNull()
        resolvingStream = false
        if (resolved != null) {
            directStreamSource = resolved
            showDirectPlayer = true
        } else {
            errorMessage = "Could not start direct stream for this link"
        }
    }

    LaunchedEffect(url, retryTrigger) {
        fetching = true
        errorMessage = null
        val result =
            withContext(Dispatchers.IO) {
                DownloadUtil.getPlaylistOrVideoInfo(
                    playlistURL = url,
                    downloadPreferences = DownloadUtil.DownloadPreferences.createFromPreferences(),
                )
            }
        result
            .onSuccess { info ->
                when (info) {
                    is PlaylistResult -> {
                        playlistInfo = info
                        videoInfo = null
                    }
                    is VideoInfo -> {
                        videoInfo = info
                        playlistInfo = null
                    }
                    else -> errorMessage = "Unsupported result type"
                }
            }
            .onFailure { errorMessage = it.message ?: "Could not fetch media info" }
        fetching = false
    }

    val basePreferences = remember(url) { DownloadUtil.DownloadPreferences.createFromPreferences() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                fetching -> {
                    PremiumFetchingContent(url = url, onCancel = onDismiss)
                }

                errorMessage != null -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { retryTrigger++ },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                        Button(
                            onClick = { onDownload(basePreferences) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Download anyway")
                        }
                    }
                }

                playlistInfo != null -> {
                    val pl = playlistInfo ?: return@Column
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 3.dp,
                        color =
                            if (isGradientDark) GradientDarkColors.GlassSurface.copy(alpha = 0.06f)
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(shape = CircleShape, color = platform.accentColor.copy(alpha = 0.14f)) {
                                Text(
                                    text = "${platform.name} • Playlist",
                                    color = platform.accentColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                            Text(
                                text = pl.title.orEmpty().ifBlank { "Playlist" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${pl.entries?.size ?: 0} videos found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = {
                                    onDownload(basePreferences.copy(downloadPlaylist = true))
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                            ) {
                                Text("Download whole playlist")
                            }
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Pick videos manually from Batch import")
                            }
                        }
                    }
                }

                else -> {
                    val info = videoInfo ?: return@Column

                    // ---------- Rich preview card ----------
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 3.dp,
                        color =
                            if (isGradientDark) GradientDarkColors.GlassSurface.copy(alpha = 0.06f)
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier =
                            Modifier.fillMaxWidth().then(
                                if (isGradientDark) {
                                    Modifier.border(
                                        1.dp,
                                        GradientDarkColors.GlassWhiteBorder,
                                        RoundedCornerShape(18.dp),
                                    )
                                } else Modifier
                            ),
                    ) {
                        Column {
                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = info.thumbnail,
                                    contentDescription = info.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                                )
                                Box(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .background(
                                                Brush.verticalGradient(
                                                    0f to Color.Transparent,
                                                    1f to Color.Black.copy(alpha = 0.55f),
                                                )
                                                ,
                                            ),
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                                ) {
                                    Text(
                                        text = platform.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    )
                                }
                                info.durationString?.let { dur ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Black.copy(alpha = 0.72f),
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                                    ) {
                                        Text(
                                            text = dur,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = info.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text =
                                        listOf(
                                                info.uploader.orEmpty().ifBlank { info.channel.orEmpty() },
                                                info.viewCount?.takeIf { it > 0 }
                                                    ?.let { "${it / 1000}K views" }
                                                    .orEmpty(),
                                            )
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

                    // ---------- Download-as presets ----------
                    Text(
                        text = "Download as",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        PresetChip(
                            selected = !audioOnly && selectedVideoPreset == 0,
                            label = "Video • Best",
                            onClick = {
                                audioOnly = false
                                selectedVideoPreset = 0
                                selectedAudioPreset = -1
                            },
                        )
                        PresetChip(
                            selected = audioOnly && selectedAudioPreset == 0,
                            label = "Audio • Best",
                            onClick = {
                                audioOnly = true
                                selectedAudioPreset = 0
                                selectedVideoPreset = -1
                            },
                        )
                    }
                    Text(
                        text = "Video quality",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf(
                            3 to "1080p",
                            4 to "720p",
                            5 to "480p",
                            6 to "360p",
                            1 to "2160p",
                            2 to "1440p",
                        ).forEach { (code, label) ->
                            PresetChip(
                                selected = !audioOnly && selectedVideoPreset == code,
                                label = label,
                                onClick = {
                                    audioOnly = false
                                    selectedVideoPreset = code
                                    selectedAudioPreset = -1
                                },
                            )
                        }
                    }
                    Text(
                        text = "Audio quality",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf(
                            0 to "Best",
                            1 to "High • 192k",
                            2 to "Medium • 128k",
                            3 to "Low • 64k",
                        ).forEach { (code, label) ->
                            PresetChip(
                                selected = audioOnly && selectedAudioPreset == code,
                                label = label,
                                onClick = {
                                    audioOnly = true
                                    selectedAudioPreset = code
                                    selectedVideoPreset = -1
                                },
                            )
                        }
                    }

                    // ---------- Real quality grid mined from fetched formats ----------
                    val videoHeights =
                        info.formats
                            .orEmpty()
                            .filter { it.containsVideo() && (it.height ?: 0.0) > 0.0 }
                            .mapNotNull { it.getResolutionLabel() }
                            .distinct()
                            .take(8)
                    if (videoHeights.isNotEmpty()) {
                        Text(
                            text = "Available for this video",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            videoHeights.forEach { label ->
                                val code =
                                    when (label) {
                                        "4K" -> 1
                                        "2K" -> 2
                                        "1080p" -> 3
                                        "720p" -> 4
                                        "480p" -> 5
                                        "360p" -> 6
                                        else -> -1
                                    }
                                PresetChip(
                                    selected = !audioOnly && code > 0 && selectedVideoPreset == code,
                                    label = label,
                                    accent = platform.accentColor,
                                    enabled = code > 0,
                                    onClick = {
                                        if (code > 0) {
                                            audioOnly = false
                                            selectedVideoPreset = code
                                            selectedAudioPreset = -1
                                        }
                                    },
                                )
                            }
                        }
                    }

                    // ---------- Actions ----------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilledTonalButton(
                            onClick = { triggerDirectStream++ },
                            enabled = !resolvingStream,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Icon(Icons.Outlined.PlayCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (resolvingStream) "Loading..." else "Stream Now")
                        }
                        Button(
                            onClick = {
                                val prefs =
                                    basePreferences
                                        .copy(
                                            extractAudio = audioOnly,
                                            videoResolution =
                                                if (!audioOnly && selectedVideoPreset >= 0) selectedVideoPreset
                                                else basePreferences.videoResolution,
                                            audioQuality =
                                                if (audioOnly && selectedAudioPreset >= 0) selectedAudioPreset
                                                else basePreferences.audioQuality,
                                        )
                                onDownload(prefs)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (audioOnly) "Audio" else "Download",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    val streamSrc = if (showDirectPlayer) directStreamSource else null
    if (streamSrc != null) {
        val player = remember { MiniPlayerManager.getOrCreatePlayer(context) }
        var selectedVariant by remember(streamSrc) { mutableIntStateOf(0) }
        var isLandscape by remember { mutableStateOf(true) }

        DisposableEffect(streamSrc) {
            val v = streamSrc.variants.first()
            MiniPlayerManager.playStream(
                context = context,
                streamUrl = v.url,
                title = streamSrc.title,
                uploader = streamSrc.uploader,
                thumbnailUrl = streamSrc.thumbnailUrl,
                sourceUrl = streamSrc.originalUrl,
                audioUrl = v.audioUrl,
            )
            val listener =
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(
                            context,
                            error.localizedMessage ?: "Stream playback failed",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        if (videoSize.width > 0 && videoSize.height > 0) {
                            isLandscape = videoSize.width >= videoSize.height
                        }
                    }
                }
            player.addListener(listener)
            val activity = context as? Activity
            val window = activity?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                player.removeListener(listener)
                controller?.show(WindowInsetsCompat.Type.systemBars())
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        LaunchedEffect(isLandscape) {
            val activity = context as? Activity ?: return@LaunchedEffect
            activity.requestedOrientation =
                if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        Dialog(
            onDismissRequest = { showDirectPlayer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                GlassPlayerSurface(
                    player = player,
                    title = streamSrc.title,
                    uploader = streamSrc.uploader,
                    isFullscreen = true,
                    variants = streamSrc.variants,
                    selectedVariant = selectedVariant,
                    onSelectVariant = { idx ->
                        selectedVariant = idx
                        val v = streamSrc.variants[idx]
                        MiniPlayerManager.playStream(
                            context = context,
                            streamUrl = v.url,
                            title = streamSrc.title,
                            uploader = streamSrc.uploader,
                            thumbnailUrl = streamSrc.thumbnailUrl,
                            sourceUrl = streamSrc.originalUrl,
                            positionMs = player.currentPosition,
                            audioUrl = v.audioUrl,
                        )
                    },
                    onExitFullscreen = { showDirectPlayer = false },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    selected: Boolean,
    label: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = accent.copy(alpha = 0.2f),
                selectedLabelColor = accent,
            ),
    )
}
