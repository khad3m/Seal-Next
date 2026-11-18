package com.junkfood.seal.ui.page.stream

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import com.junkfood.seal.util.LowRamOptimizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MiniPlayerState(
    val isVisible: Boolean = false,
    val title: String = "",
    val uploader: String = "",
    val thumbnailUrl: String? = null,
    val sourceUrl: String = "",
    val isPlaying: Boolean = false,
)

/**
 * Holds a single shared low-RAM-optimized ExoPlayer instance so StreamPage playback can
 * continue seamlessly in the background and appear as an app-wide floating mini-player.
 */
object MiniPlayerManager {

    private const val STREAM_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Mobile Safari/537.36"

    private var player: ExoPlayer? = null

    private val _state = MutableStateFlow(MiniPlayerState())
    val state = _state.asStateFlow()

    fun getOrCreatePlayer(context: Context): ExoPlayer {
        player?.let { return it }
        return LowRamOptimizer.createOptimizedExoPlayer(context.applicationContext).also { p ->
            p.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _state.update { it.copy(isPlaying = isPlaying) }
                    }
                }
            )
            player = p
        }
    }

    @OptIn(UnstableApi::class)
    fun playStream(
        context: Context,
        streamUrl: String,
        title: String,
        uploader: String,
        thumbnailUrl: String?,
        sourceUrl: String,
        speed: Float = 1.0f,
        positionMs: Long = 0L,
        audioUrl: String? = null,
    ) {
        val p = getOrCreatePlayer(context)
        val httpFactory =
            DefaultHttpDataSource.Factory()
                .setUserAgent(STREAM_USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(20_000)

        val mediaSourceFactory = DefaultMediaSourceFactory(httpFactory)
        if (!audioUrl.isNullOrBlank()) {
            val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(streamUrl))
            val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
            p.setMediaSource(MergingMediaSource(videoSource, audioSource), positionMs)
        } else {
            val source = mediaSourceFactory.createMediaSource(MediaItem.fromUri(streamUrl))
            p.setMediaSource(source, positionMs)
        }
        p.setPlaybackSpeed(speed)
        p.prepare()
        p.playWhenReady = true
        _state.value =
            MiniPlayerState(
                isVisible = true,
                title = title,
                uploader = uploader,
                thumbnailUrl = thumbnailUrl,
                sourceUrl = sourceUrl,
                isPlaying = true,
            )
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun dismissAndStop() {
        player?.stop()
        player?.release()
        player = null
        _state.value = MiniPlayerState()
    }
}
