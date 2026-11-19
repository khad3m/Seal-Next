package com.junkfood.seal.util

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.junkfood.seal.App
import com.junkfood.seal.Downloader
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Zero-dependency Wi-Fi Queue Manager backed by MMKV + ConnectivityManager.NetworkCallback.
 * Queued URLs automatically start downloading as soon as a Wi-Fi network is connected.
 */
object WifiQueueManager {

    private const val KEY_WIFI_QUEUE = "wifi_scheduled_urls"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var registered = false

    private val _queuedUrls = MutableStateFlow<List<String>>(loadQueue())
    val queuedUrls = _queuedUrls.asStateFlow()

    private fun loadQueue(): List<String> =
        runCatching {
                MMKV.defaultMMKV()
                    .decodeStringSet(KEY_WIFI_QUEUE, emptySet())
                    ?.toList()
                    .orEmpty()
            }
            .getOrDefault(emptyList())

    private fun saveQueue(list: List<String>) {
        runCatching { MMKV.defaultMMKV().encode(KEY_WIFI_QUEUE, list.toSet()) }
        _queuedUrls.value = list
    }

    fun enqueueForWifi(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        ensureRegistered()
        if (isOnWifi()) {
            Downloader.getInfoAndDownload(trimmed)
            return
        }
        val updated = (_queuedUrls.value + trimmed).distinct()
        saveQueue(updated)
    }

    fun removeFromQueue(url: String) {
        saveQueue(_queuedUrls.value - url)
    }

    fun clearQueue() {
        saveQueue(emptyList())
    }

    fun isOnWifi(): Boolean {
        val cm = App.connectivityManager
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun flushIfOnWifi() {
        if (!isOnWifi()) return
        flushNow()
    }

    private fun flushNow() {
        val items = _queuedUrls.value
        if (items.isEmpty()) return
        saveQueue(emptyList())
        scope.launch {
            items.forEach { url ->
                // bug #7: respect the network restriction preference, and stagger the
                // flush so a reconnect doesn't spawn N yt-dlp processes at once (which
                // spiked RAM/CPU and hung low-end devices).
                if (!PreferenceUtil.isNetworkAvailableForDownload()) return@launch
                Downloader.getInfoAndDownload(url)
                delay(2000L)
            }
        }
    }

    fun ensureRegistered() {
        if (registered) return
        synchronized(this) {
            if (registered) return
            runCatching {
                val req =
                    NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build()
                App.connectivityManager.registerNetworkCallback(
                    req,
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            val caps = App.connectivityManager.getNetworkCapabilities(network)
                            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                                flushNow()
                            }
                        }
                    },
                )
                registered = true
            }
        }
    }
}
