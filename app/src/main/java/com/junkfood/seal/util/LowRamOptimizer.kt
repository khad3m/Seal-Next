package com.junkfood.seal.util

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.core.content.getSystemService
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import coil3.SingletonImageLoader
import com.tencent.mmkv.MMKV
import java.io.File

/**
 * Adaptive memory & playback optimizer for low-RAM Android devices (2 GB – 3 GB RAM)
 * plus a user-controllable Low-RAM Lite Mode and orphaned `.part` temp cleaner.
 */
object LowRamOptimizer {

    private const val KEY_LITE_MODE = "low_ram_lite_mode"

    @Volatile
    private var cachedIsLowRam: Boolean? = null

    @Volatile
    private var galleryThumbCache: LruCache<String, Bitmap>? = null

    fun isLowRamDevice(context: Context): Boolean {
        cachedIsLowRam?.let { return it }
        val am = context.applicationContext.getSystemService<ActivityManager>()
        val result =
            if (am != null) {
                am.isLowRamDevice || am.memoryClass <= 192
            } else {
                false
            }
        cachedIsLowRam = result
        return result
    }

    fun isManualLiteMode(): Boolean =
        runCatching { MMKV.defaultMMKV().decodeBool(KEY_LITE_MODE, false) }.getOrDefault(false)

    fun setManualLiteMode(enabled: Boolean) {
        runCatching { MMKV.defaultMMKV().encode(KEY_LITE_MODE, enabled) }
        galleryThumbCache?.evictAll()
    }

    fun isLowRamMode(context: Context): Boolean = isManualLiteMode() || isLowRamDevice(context)

    fun shouldReduceAnimations(context: Context): Boolean = isLowRamMode(context)

    fun recommendedMaxConcurrentDownloads(context: Context): Int =
        if (isLowRamMode(context)) 1 else 3

    fun coilMemoryCachePercent(context: Context): Double =
        if (isLowRamMode(context)) 0.10 else 0.20

    fun thumbnailCacheCapacity(context: Context): Int =
        if (isLowRamMode(context)) 12 else 24

    fun getGalleryThumbCache(context: Context): LruCache<String, Bitmap> {
        galleryThumbCache?.let { return it }
        val capacity = thumbnailCacheCapacity(context)
        return LruCache<String, Bitmap>(capacity).also { galleryThumbCache = it }
    }

    /**
     * Builds an ExoPlayer instance with a memory-bounded [DefaultLoadControl] so 1080p streams
     * and local files do not trigger OutOfMemoryError on low-RAM devices.
     */
    @OptIn(UnstableApi::class)
    fun createOptimizedExoPlayer(context: Context): ExoPlayer {
        val lowRam = isLowRamMode(context)
        val loadControl =
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ if (lowRam) 5_000 else 12_000,
                    /* maxBufferMs = */ if (lowRam) 15_000 else 30_000,
                    /* bufferForPlaybackMs = */ if (lowRam) 1_500 else 2_000,
                    /* bufferForPlaybackAfterRebufferMs = */ if (lowRam) 3_000 else 4_000,
                )
                .setPrioritizeTimeOverSizeThresholds(!lowRam)
                .build()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
    }

    /**
     * Evicts image & thumbnail caches when Android signals memory pressure or UI backgrounding.
     */
    fun trimMemoryCaches(context: Context, level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        ) {
            runCatching {
                SingletonImageLoader.get(context.applicationContext).memoryCache?.clear()
            }
            galleryThumbCache?.evictAll()
        }
    }

    fun clearAllCaches(context: Context) {
        runCatching {
            SingletonImageLoader.get(context.applicationContext).memoryCache?.clear()
        }
        galleryThumbCache?.evictAll()
    }

    /**
     * Removes orphaned `.part` and `.ytdl` temp files from download directories.
     * Returns Pair(deletedCount, freedBytes).
     */
    fun cleanOrphanedPartFiles(dirs: List<String>): Pair<Int, Long> {
        var count = 0
        var bytes = 0L
        dirs.distinct()
            .map { File(it) }
            .filter { it.exists() && it.isDirectory }
            .forEach { dir ->
                dir.walkTopDown().maxDepth(3).forEach { file ->
                    if (file.isFile) {
                        val lower = file.name.lowercase()
                        if (lower.endsWith(".part") || lower.endsWith(".ytdl") || lower.endsWith(".temp")) {
                            val len = file.length()
                            if (runCatching { file.delete() }.getOrDefault(false)) {
                                count++
                                bytes += len
                            }
                        }
                    }
                }
            }
        return count to bytes
    }
}
