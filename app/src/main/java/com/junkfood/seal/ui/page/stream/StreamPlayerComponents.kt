package com.junkfood.seal.ui.page.stream

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import coil3.compose.AsyncImage
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.VideoInfo
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

data class StreamVariant(
    val label: String,
    val url: String,
    val isAudioOnly: Boolean = false,
    val audioUrl: String? = null,
    val meta: String = "",
)

data class StreamSource(
    val title: String,
    val uploader: String,
    val durationSec: Long,
    val thumbnailUrl: String?,
    val originalUrl: String,
    val variants: List<StreamVariant>,
)

@AndroidXOptIn(UnstableApi::class)
enum class PlayerResizeMode(val label: String, val mode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ;

    fun next(): PlayerResizeMode =
        when (this) {
            FIT -> FILL
            FILL -> STRETCH
            STRETCH -> FIT
        }
}

object PlaybackResumeStore {
    private fun key(url: String): String = "stream_resume_${url.hashCode()}"

    fun save(url: String, positionMs: Long, durationMs: Long) {
        if (url.isBlank() || durationMs <= 15_000L) return
        val keep = if (positionMs in 5_000L..(durationMs - 10_000L)) positionMs else 0L
        runCatching { MMKV.defaultMMKV().encode(key(url), keep) }
    }

    fun get(url: String): Long {
        if (url.isBlank()) return 0L
        return runCatching { MMKV.defaultMMKV().decodeLong(key(url), 0L) }.getOrDefault(0L)
    }
}

@Volatile
private var newPipeInitialized = false

fun ensureNewPipeReady() {
    if (!newPipeInitialized) {
        synchronized(NewPipeDownloader) {
            if (!newPipeInitialized) {
                NewPipe.init(NewPipeDownloader)
                newPipeInitialized = true
            }
        }
    }
}

private val STREAM_URL_REGEX = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

fun extractStreamUrl(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    STREAM_URL_REGEX.find(trimmed)?.value?.let { found ->
        return found.trimEnd(',', '.', ')', ']', '>', '"', '\'')
    }
    val lower = trimmed.lowercase()
    val knownDomain =
        listOf(
            "youtube.com/",
            "youtu.be/",
            "music.youtube.com/",
            "m.youtube.com/",
            "tiktok.com/",
            "instagram.com/",
            "instagr.am/",
            "facebook.com/",
            "fb.watch/",
            "fb.com/",
            "x.com/",
            "twitter.com/",
        ).any { lower.contains(it) }
    return if (knownDomain) "https://${trimmed.substringBefore(' ')}" else null
}

fun normalizeYouTubeStreamUrl(url: String): String {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return url
    val host = uri.host?.lowercase().orEmpty()
    if (!host.contains("youtube.com") && !host.contains("youtu.be")) return url

    if (host.contains("youtu.be")) {
        val id = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: return url
        return "https://www.youtube.com/watch?v=$id"
    }
    val path = uri.path.orEmpty()
    if (path.startsWith("/shorts/") || path.startsWith("/live/")) {
        val id = path.split('/').filter { it.isNotBlank() }.getOrNull(1)
        if (!id.isNullOrBlank()) {
            return "https://www.youtube.com/watch?v=$id"
        }
    }
    val v = uri.getQueryParameter("v")
    if (!v.isNullOrBlank()) {
        return "https://www.youtube.com/watch?v=$v"
    }
    return url.replace("://m.youtube.com", "://www.youtube.com")
}

private fun formatApproxSizeOrBitrate(bytes: Double?, tbrKbps: Double?): String {
    if (bytes != null && bytes > 0.0) {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) "%.1f MB".format(mb) else "%.0f KB".format(bytes / 1024.0)
    }
    if (tbrKbps != null && tbrKbps > 0.0) {
        return if (tbrKbps >= 1000.0) "%.1f Mbps".format(tbrKbps / 1000.0) else "%.0f kbps".format(tbrKbps)
    }
    return ""
}

fun buildVariantsFromVideoInfo(info: VideoInfo): List<StreamVariant> {
    val allFormats =
        (info.formats.orEmpty() + info.requestedFormats.orEmpty())
            .filter { it.url?.startsWith("http") == true }

    val bestAudio =
        allFormats
            .filter { it.isAudioOnly() }
            .maxByOrNull { it.abr ?: it.tbr ?: 0.0 }

    val muxedNonHls =
        allFormats
            .filter { it.containsVideo() && it.containsAudio() && it.ext != "m3u8" }
            .sortedByDescending { it.height ?: 0.0 }
            .mapNotNull { fmt ->
                val u = fmt.url ?: return@mapNotNull null
                StreamVariant(
                    label = fmt.height?.let { h -> "${h.toInt()}p" } ?: "Auto",
                    url = u,
                    meta = formatApproxSizeOrBitrate(fmt.fileSize ?: fmt.fileSizeApprox, fmt.tbr),
                )
            }

    val splitDash =
        if (bestAudio != null) {
            allFormats
                .filter { it.containsVideo() && it.isVideoOnly() && it.ext != "m3u8" }
                .sortedByDescending { it.height ?: 0.0 }
                .mapNotNull { fmt ->
                    val u = fmt.url ?: return@mapNotNull null
                    StreamVariant(
                        label = fmt.height?.let { h -> "${h.toInt()}p" } ?: "HD",
                        url = u,
                        audioUrl = bestAudio.url,
                        meta = formatApproxSizeOrBitrate(fmt.fileSize ?: fmt.fileSizeApprox, fmt.tbr),
                    )
                }
        } else {
            emptyList()
        }

    val hlsFormats =
        allFormats
            .filter { it.containsVideo() && it.ext == "m3u8" }
            .sortedByDescending { it.height ?: 0.0 }
            .mapNotNull { fmt ->
                val u = fmt.url ?: return@mapNotNull null
                StreamVariant(
                    label = fmt.height?.let { h -> "${h.toInt()}p (HLS)" } ?: "Auto (HLS)",
                    url = u,
                    meta = formatApproxSizeOrBitrate(null, fmt.tbr),
                )
            }

    val variants = (muxedNonHls + splitDash + hlsFormats).distinctBy { it.label }.toMutableList()
    bestAudio?.url?.let { audioUrl ->
        variants.add(
            StreamVariant(
                label = "Audio Only",
                url = audioUrl,
                isAudioOnly = true,
                meta = formatApproxSizeOrBitrate(bestAudio.fileSize ?: bestAudio.fileSizeApprox, bestAudio.abr ?: bestAudio.tbr),
            )
        )
    }
    return variants
}

fun streamSourceFromVideoInfo(info: VideoInfo, fallbackUrl: String): StreamSource? {
    val variants = buildVariantsFromVideoInfo(info)
    if (variants.isEmpty()) return null
    return StreamSource(
        title = info.title,
        uploader = info.uploader.orEmpty().ifBlank { info.channel.orEmpty() },
        durationSec = (info.duration ?: 0.0).toLong(),
        thumbnailUrl = info.thumbnail,
        originalUrl = info.webpageUrl ?: fallbackUrl,
        variants = variants,
    )
}

fun friendlyStreamError(th: Throwable?): String {
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

private suspend fun resolveViaYtDlp(url: String): StreamSource {
    val info = DownloadUtil.fetchVideoInfoFromUrl(url).getOrThrow()
    return streamSourceFromVideoInfo(info, url)
        ?: error("No directly playable stream found (cookies/login may be required)")
}

private suspend fun resolveViaNewPipe(url: String): StreamSource {
    ensureNewPipeReady()
    val info = StreamInfo.getInfo(ServiceList.YouTube, url)
    val bestAudio =
        info.audioStreams
            .filter { it.content.isNotBlank() }
            .maxByOrNull { it.averageBitrate }

    val muxed =
        info.videoStreams
            .filter { !it.isVideoOnly && it.content.isNotBlank() }
            .sortedByDescending { it.height }
            .map {
                StreamVariant(
                    label = if (it.height > 0) "${it.height}p" else "Auto",
                    url = it.content,
                    meta = if (it.bitrate > 0) formatApproxSizeOrBitrate(null, it.bitrate / 1000.0) else "",
                )
            }

    val splitDash =
        if (bestAudio != null) {
            info.videoOnlyStreams
                .filter { it.content.isNotBlank() }
                .sortedByDescending { it.height }
                .map {
                    StreamVariant(
                        label = if (it.height > 0) "${it.height}p" else "HD",
                        url = it.content,
                        audioUrl = bestAudio.content,
                        meta = if (it.bitrate > 0) formatApproxSizeOrBitrate(null, it.bitrate / 1000.0) else "",
                    )
                }
        } else {
            emptyList()
        }

    val list = (muxed + splitDash).distinctBy { it.label }.toMutableList()
    if (!info.hlsUrl.isNullOrBlank()) {
        list.add(StreamVariant("Auto (HLS)", info.hlsUrl))
    }
    bestAudio?.let { audio ->
        list.add(
            StreamVariant(
                label = "Audio Only",
                url = audio.content,
                isAudioOnly = true,
                meta = if (audio.averageBitrate > 0) "${audio.averageBitrate} kbps" else "",
            )
        )
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

suspend fun resolveStreamForPlayer(rawInput: String): StreamSource =
    withContext(Dispatchers.IO) {
        val extracted = extractStreamUrl(rawInput) ?: rawInput.trim()
        val rawUrl =
            if (extracted.startsWith("http://") || extracted.startsWith("https://")) extracted
            else "https://$extracted"
        val url = normalizeYouTubeStreamUrl(rawUrl)
        val host = runCatching { java.net.URI(url).host.orEmpty().lowercase() }.getOrDefault("")

        when {
            host.contains("tiktok.com") ||
                host.contains("instagram.com") ||
                host.contains("instagr.am") ||
                host.contains("facebook.com") ||
                host.contains("fb.watch") ||
                host.contains("fb.com") ||
                host.contains("x.com") ||
                host.contains("twitter.com") ->
                runCatching { resolveViaYtDlp(url) }
                    .getOrElse { throw Exception(friendlyStreamError(it)) }

            else -> {
                val npResult = runCatching { resolveViaNewPipe(url) }
                npResult.getOrNull()
                    ?: runCatching { resolveViaYtDlp(url) }
                        .getOrElse { ytErr ->
                            throw Exception(friendlyStreamError(ytErr))
                        }
            }
        }
    }

private data class StreamPlayerUiSnapshot(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
)

private enum class StreamGestureHud { None, Seek, Volume, Brightness, ScreenMode }

private fun formatStreamTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    return if (totalSec >= 3600L) {
        "%d:%02d:%02d".format(totalSec / 3600L, (totalSec % 3600L) / 60L, totalSec % 60L)
    } else {
        "%d:%02d".format(totalSec / 60L, totalSec % 60L)
    }
}

@AndroidXOptIn(UnstableApi::class)
@Composable
fun GlassPlayerSurface(
    player: Player,
    title: String,
    uploader: String,
    isFullscreen: Boolean,
    variants: List<StreamVariant> = emptyList(),
    selectedVariant: Int = 0,
    onSelectVariant: (Int) -> Unit = {},
    sourceUrl: String = "",
    thumbnailUrl: String? = null,
    modifier: Modifier = Modifier,
    onEnterFullscreen: () -> Unit = {},
    onExitFullscreen: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity
    // bug #3: brightness/volume gestures must target the window that actually displays
    // the player. In fullscreen the player lives inside a Compose Dialog (its own
    // window) — writing screenBrightness on the activity window did nothing because the
    // focused dialog window sits on top.
    val playerWindow =
        (LocalView.current.parent as? DialogWindowProvider)?.window ?: activity?.window
    // bug #3: don't leave a stuck brightness override behind after the player closes.
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                val w = playerWindow ?: return@runCatching
                val attrs = w.attributes
                attrs.screenBrightness =
                    android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                w.attributes = attrs
            }
        }
    }
    val ui by produceState(StreamPlayerUiSnapshot(), player) {
        while (true) {
            // bug #3: the player can be released by MiniPlayerManager / route changes
            // while this poll loop is still composed — guard every access so a released
            // player throws no IllegalStateException into composition.
            runCatching {
                value =
                    StreamPlayerUiSnapshot(
                        isPlaying = player.isPlaying,
                        isBuffering = player.playbackState == Player.STATE_BUFFERING,
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L),
                        bufferedMs = player.bufferedPosition.coerceAtLeast(0L),
                    )
            }
            delay(250L)
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isTemp2x by remember { mutableStateOf(false) }
    var savedSpeedBeforeHold by remember { mutableFloatStateOf(1.0f) }
    var gestureHud by remember { mutableStateOf(StreamGestureHud.None) }
    var feedbackValue by remember { mutableFloatStateOf(0f) }
    var scrubTargetMs by remember { mutableLongStateOf(-1L) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showTracksSheet by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }
    var sleepMinutesLeft by remember { mutableIntStateOf(0) }
    var resizeMode by remember { mutableStateOf(PlayerResizeMode.FIT) }
    var orientationLocked by remember { mutableStateOf<Int?>(null) }

    // Periodically persist playback position for Playback Resume
    LaunchedEffect(sourceUrl, ui.isPlaying) {
        while (sourceUrl.isNotBlank() && ui.isPlaying) {
            runCatching { PlaybackResumeStore.save(sourceUrl, player.currentPosition, player.duration) }
            delay(3000L)
        }
    }

    // Sleep timer countdown
    LaunchedEffect(sleepMinutesLeft) {
        if (sleepMinutesLeft > 0) {
            delay(60_000L)
            sleepMinutesLeft--
            if (sleepMinutesLeft == 0) {
                player.pause()
            }
        }
    }

    LaunchedEffect(controlsVisible, ui.isPlaying, isLocked) {
        if (controlsVisible && (ui.isPlaying || isLocked)) {
            delay(3500L)
            controlsVisible = false
        }
    }
    LaunchedEffect(gestureHud) {
        if (gestureHud == StreamGestureHud.ScreenMode) {
            delay(900L)
            gestureHud = StreamGestureHud.None
        }
    }

    Box(
        modifier =
            (if (isFullscreen) modifier else modifier.fillMaxWidth().aspectRatio(16f / 9f))
                .clip(RoundedCornerShape(if (isFullscreen) 0.dp else 18.dp))
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
            update = { pv -> pv.resizeMode = resizeMode.mode },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = controlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.62f),
                                0.35f to Color.Transparent,
                                0.65f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.78f),
                            )
                        )
            )
        }

        // Full Gesture Layer: Tap, Double-Tap ±10s, Long-Press 2x Speed, Horizontal Seek, Brightness/Volume
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .pointerInput(isLocked) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                if (isLocked) return@detectTapGestures
                                val third = size.width / 3f
                                val delta =
                                    if (offset.x < third) -10_000L else if (offset.x > third * 2) 10_000L else 0L
                                if (delta != 0L) {
                                    val target =
                                        (player.currentPosition + delta)
                                            .coerceIn(0L, player.duration.coerceAtLeast(0L))
                                    player.seekTo(target)
                                    scrubTargetMs = target
                                    gestureHud = StreamGestureHud.Seek
                                }
                            },
                            onLongPress = {
                                if (!isLocked && player.isPlaying) {
                                    savedSpeedBeforeHold = player.playbackParameters.speed
                                    player.setPlaybackSpeed(2.0f)
                                    isTemp2x = true
                                }
                            },
                        )
                    }
                    .pointerInput(isLocked) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            waitForUpOrCancellation()
                            if (isTemp2x) {
                                player.setPlaybackSpeed(savedSpeedBeforeHold)
                                isTemp2x = false
                            }
                        }
                    }
                    .pointerInput(isLocked) {
                        // bug #3: vertical (brightness/volume) is registered BEFORE the
                        // horizontal seek detector — when it came second, drags with a
                        // slight horizontal bias lost the slop race and brightness never
                        // triggered at all.
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (isLocked) return@detectVerticalDragGestures
                                change.consume()
                                when {
                                    change.position.x < size.width / 2f -> {
                                        val window = playerWindow ?: return@detectVerticalDragGestures
                                        val attrs = window.attributes
                                        val current =
                                            if (attrs.screenBrightness < 0f) 0.5f else attrs.screenBrightness
                                        val next = (current - dragAmount / size.height).coerceIn(0.02f, 1f)
                                        attrs.screenBrightness = next
                                        window.attributes = attrs
                                        feedbackValue = next
                                        gestureHud = StreamGestureHud.Brightness
                                    }
                                    else -> {
                                        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                        val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val step = if (dragAmount < -10f) 1 else if (dragAmount > 10f) -1 else 0
                                        if (step != 0) {
                                            val nextVol = (curVol + step).coerceIn(0, maxVol)
                                            am.setStreamVolume(AudioManager.STREAM_MUSIC, nextVol, 0)
                                            feedbackValue = nextVol.toFloat() / maxVol.toFloat()
                                            gestureHud = StreamGestureHud.Volume
                                        } else {
                                            feedbackValue = curVol.toFloat() / maxVol.toFloat()
                                            gestureHud = StreamGestureHud.Volume
                                        }
                                    }
                                }
                            },
                            onDragEnd = { gestureHud = StreamGestureHud.None },
                            onDragCancel = { gestureHud = StreamGestureHud.None },
                        )
                    }
                    .pointerInput(isLocked) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                if (!isLocked && ui.durationMs > 0) scrubTargetMs = ui.positionMs
                            },
                            onDragEnd = {
                                if (!isLocked && scrubTargetMs >= 0) player.seekTo(scrubTargetMs)
                                scrubTargetMs = -1L
                                gestureHud = StreamGestureHud.None
                            },
                            onDragCancel = {
                                scrubTargetMs = -1L
                                gestureHud = StreamGestureHud.None
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (isLocked) return@detectHorizontalDragGestures
                                change.consume()
                                if (ui.durationMs > 0) {
                                    val step = dragAmount / size.width * ui.durationMs
                                    scrubTargetMs =
                                        ((if (scrubTargetMs < 0) ui.positionMs else scrubTargetMs) + step.toLong())
                                            .coerceIn(0L, ui.durationMs)
                                    gestureHud = StreamGestureHud.Seek
                                }
                            },
                        )
                    }
        )

        if (ui.isBuffering && gestureHud == StreamGestureHud.None) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.align(Alignment.Center).size(42.dp),
            )
        }

        // Temporary 2x Speed Pill
        AnimatedVisibility(
            visible = isTemp2x,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
        ) {
            Surface(shape = CircleShape, color = Color(0xCC121216)) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.FastForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("2.0× Speed", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Lock Screen Unlock Pill
        AnimatedVisibility(
            visible = isLocked && controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        ) {
            Surface(
                onClick = { isLocked = false },
                shape = CircleShape,
                color = Color(0xCC121216),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.LockOpen, "Unlock", tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("Tap to unlock", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Gesture HUD Pill
        AnimatedVisibility(
            visible = gestureHud != StreamGestureHud.None && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xCC121216),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (gestureHud == StreamGestureHud.Seek && !thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 128.dp, height = 72.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val icon =
                            when (gestureHud) {
                                StreamGestureHud.Volume -> Icons.Outlined.VolumeUp
                                StreamGestureHud.Brightness -> Icons.Outlined.WbSunny
                                StreamGestureHud.ScreenMode -> Icons.Outlined.AspectRatio
                                else -> Icons.Outlined.Forward10
                            }
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(
                            text =
                                when (gestureHud) {
                                    StreamGestureHud.Seek ->
                                        formatStreamTime(if (scrubTargetMs >= 0) scrubTargetMs else ui.positionMs)
                                    StreamGestureHud.Volume -> "Volume ${(feedbackValue * 100).toInt()}%"
                                    StreamGestureHud.Brightness -> "Brightness ${(feedbackValue * 100).toInt()}%"
                                    StreamGestureHud.ScreenMode -> "Screen: ${resizeMode.label}"
                                    StreamGestureHud.None -> ""
                                },
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (gestureHud == StreamGestureHud.Volume || gestureHud == StreamGestureHud.Brightness) {
                        LinearProgressIndicator(
                            progress = { feedbackValue.coerceIn(0f, 1f) },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.width(110.dp).height(4.dp).clip(CircleShape),
                        )
                    }
                }
            }
        }

        // Top Bar Overlay
        AnimatedVisibility(
            visible = controlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text(
                            text = title,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (uploader.isNotBlank() || sleepMinutesLeft > 0) {
                            Text(
                                text =
                                    listOfNotNull(
                                        uploader.takeIf { it.isNotBlank() },
                                        sleepMinutesLeft.takeIf { it > 0 }?.let { "Sleep ${it}m" },
                                    ).joinToString("  •  "),
                                color = Color.White.copy(alpha = 0.78f),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (isFullscreen) {
                    GlassPlayerButton(size = 34.dp, onClick = {
                        val next =
                            if (orientationLocked == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }
                        orientationLocked = next
                        activity?.requestedOrientation = next
                    }) {
                        Icon(Icons.Outlined.ScreenRotation, "Orientation lock", tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                }
                GlassPlayerButton(size = 34.dp, onClick = { showSleepSheet = true }) {
                    Icon(
                        Icons.Outlined.Timer,
                        "Sleep timer",
                        tint = if (sleepMinutesLeft > 0) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(17.dp),
                    )
                }
                GlassPlayerButton(size = 34.dp, onClick = { showTracksSheet = true }) {
                    Icon(Icons.Outlined.Subtitles, "Tracks & Subtitles", tint = Color.White, modifier = Modifier.size(17.dp))
                }
                GlassPlayerButton(size = 34.dp, onClick = { showStatsSheet = true }) {
                    Icon(Icons.Outlined.Info, "Playback info", tint = Color.White, modifier = Modifier.size(17.dp))
                }
                GlassPlayerButton(size = 34.dp, onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        runCatching { activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().build()) }
                    }
                }) {
                    Icon(Icons.Outlined.PictureInPictureAlt, "Picture in picture", tint = Color.White, modifier = Modifier.size(17.dp))
                }
                GlassPlayerButton(size = 34.dp, onClick = {
                    if (isFullscreen) onExitFullscreen() else onEnterFullscreen()
                }) {
                    Icon(
                        if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }

        // Center Controls
        AnimatedVisibility(
            visible = controlsVisible && !isLocked && !ui.isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassPlayerButton(size = 50.dp, onClick = {
                    player.seekTo((player.currentPosition - 10_000L).coerceIn(0L, player.duration.coerceAtLeast(0L)))
                }) {
                    Icon(Icons.Outlined.Replay10, "Rewind 10 seconds", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                GlassPlayerButton(size = 66.dp, onClick = {
                    if (player.isPlaying) player.pause() else player.play()
                }) {
                    Icon(
                        if (ui.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (ui.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }
                GlassPlayerButton(size = 50.dp, onClick = {
                    player.seekTo((player.currentPosition + 10_000L).coerceIn(0L, player.duration.coerceAtLeast(0L)))
                }) {
                    Icon(Icons.Outlined.Forward10, "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Bottom Controls: Seekbar + Time + Lock + Fit/Fill/Stretch + Speed + Quality
        AnimatedVisibility(
            visible = controlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .then(if (isFullscreen) Modifier.navigationBarsPadding() else Modifier)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                        gestureHud = StreamGestureHud.Seek
                    },
                    onValueChangeFinished = {
                        if (scrubTargetMs >= 0) player.seekTo(scrubTargetMs)
                        scrubTargetMs = -1L
                        gestureHud = StreamGestureHud.None
                    },
                    valueRange = 0f..(if (ui.durationMs > 0) ui.durationMs.toFloat() else 1f),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                        ),
                    modifier = Modifier.fillMaxWidth().height(26.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${formatStreamTime(if (scrubTargetMs >= 0) scrubTargetMs else ui.positionMs)} / ${formatStreamTime(ui.durationMs)}",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.weight(1f))
                    GlassPlayerButton(size = 34.dp, onClick = { isLocked = true }) {
                        Icon(Icons.Outlined.Lock, "Lock controls", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Surface(
                        onClick = {
                            resizeMode = resizeMode.next()
                            gestureHud = StreamGestureHud.ScreenMode
                        },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.36f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Outlined.AspectRatio, "Screen mode", tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(resizeMode.label, color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    GlassPlayerButton(size = 34.dp, onClick = { showSpeedSheet = true }) {
                        Icon(Icons.Outlined.Speed, "Playback speed", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    if (variants.isNotEmpty()) {
                        GlassPlayerButton(size = 34.dp, onClick = { showQualitySheet = true }) {
                            Icon(Icons.Outlined.HighQuality, "Quality", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showQualitySheet && variants.isNotEmpty()) {
        Dialog(onDismissRequest = { showQualitySheet = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Stream quality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        variants.forEachIndexed { idx, v ->
                            FilterChip(
                                selected = idx == selectedVariant,
                                onClick = {
                                    onSelectVariant(idx)
                                    showQualitySheet = false
                                },
                                label = {
                                    Text(if (v.meta.isNotBlank()) "${v.label} · ${v.meta}" else v.label)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSpeedSheet) {
        Dialog(onDismissRequest = { showSpeedSheet = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Playback speed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val curSpeed = (player.playbackParameters.speed * 100).toInt() / 100f
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            FilterChip(
                                selected = curSpeed == spd,
                                onClick = {
                                    player.setPlaybackSpeed(spd)
                                    showSpeedSheet = false
                                },
                                label = { Text("${spd}x") },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSleepSheet) {
        Dialog(onDismissRequest = { showSleepSheet = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sleep timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(0 to "Off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m").forEach { (mins, lbl) ->
                            FilterChip(
                                selected = sleepMinutesLeft == mins,
                                onClick = {
                                    sleepMinutesLeft = mins
                                    showSleepSheet = false
                                },
                                label = { Text(lbl) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTracksSheet) {
        val audioLangs = remember(showTracksSheet) {
            player.currentTracks.groups
                .filter { it.type == C.TRACK_TYPE_AUDIO }
                .flatMap { grp -> (0 until grp.length).mapNotNull { i -> grp.getTrackFormat(i).language } }
                .distinct()
        }
        var subsDisabled by remember {
            mutableStateOf(player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        }
        Dialog(onDismissRequest = { showTracksSheet = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Audio & Subtitles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(
                        onClick = {
                            subsDisabled = !subsDisabled
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, subsDisabled)
                                .build()
                        }
                    ) {
                        Text(if (subsDisabled) "Enable Subtitles / CC" else "Disable Subtitles / CC")
                    }
                    if (audioLangs.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            audioLangs.forEach { lang ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        player.trackSelectionParameters = player.trackSelectionParameters
                                            .buildUpon()
                                            .setPreferredAudioLanguage(lang)
                                            .build()
                                        showTracksSheet = false
                                    },
                                    label = { Text(lang.uppercase()) },
                                )
                            }
                        }
                    } else {
                        Text("Default audio track", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showStatsSheet) {
        Dialog(onDismissRequest = { showStatsSheet = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Playback stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Resolution: ${player.videoSize.width} × ${player.videoSize.height}", style = MaterialTheme.typography.bodySmall)
                    Text("Speed: ${player.playbackParameters.speed}×", style = MaterialTheme.typography.bodySmall)
                    Text("Buffered: ${formatStreamTime(ui.bufferedMs)}", style = MaterialTheme.typography.bodySmall)
                    Text("Resize mode: ${resizeMode.label}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun GlassPlayerButton(
    size: Dp = 38.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.36f),
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
