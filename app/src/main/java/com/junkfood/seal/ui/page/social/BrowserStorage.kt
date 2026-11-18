package com.junkfood.seal.ui.page.social

import android.net.Uri
import com.tencent.mmkv.MMKV
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BrowserHistoryEntry(
    val url: String,
    val title: String,
    val visitCount: Int = 1,
    val lastVisitedMs: Long = System.currentTimeMillis(),
)

@Serializable
data class BrowserBookmark(
    val url: String,
    val title: String,
    val addedMs: Long = System.currentTimeMillis(),
)

data class BrowserTab(
    val id: Long,
    val title: String,
    val url: String,
)

object BrowserHistoryStore {
    private const val KEY_HISTORY = "browser_history_v1"
    private val json = Json { ignoreUnknownKeys = true }

    fun getAll(): List<BrowserHistoryEntry> {
        val raw = runCatching { MMKV.defaultMMKV().decodeString(KEY_HISTORY, "") }.getOrNull().orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<BrowserHistoryEntry>>(raw) }.getOrDefault(emptyList())
    }

    fun recordVisit(url: String, title: String) {
        if (url.isBlank() || url == "about:blank") return
        val cleanTitle =
            title.ifBlank { runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault(url) }
        val current = getAll().toMutableList()
        val idx = current.indexOfFirst { it.url == url }
        if (idx >= 0) {
            val prev = current[idx]
            current[idx] = prev.copy(
                title = cleanTitle,
                visitCount = prev.visitCount + 1,
                lastVisitedMs = System.currentTimeMillis(),
            )
        } else {
            current.add(0, BrowserHistoryEntry(url = url, title = cleanTitle))
        }
        val capped = current.sortedByDescending { it.lastVisitedMs }.take(200)
        runCatching { MMKV.defaultMMKV().encode(KEY_HISTORY, json.encodeToString(capped)) }
    }

    fun clear() {
        runCatching { MMKV.defaultMMKV().removeValueForKey(KEY_HISTORY) }
    }
}

object BrowserBookmarkStore {
    private const val KEY_BOOKMARKS = "browser_bookmarks_v1"
    private val json = Json { ignoreUnknownKeys = true }

    fun getAll(): List<BrowserBookmark> {
        val raw = runCatching { MMKV.defaultMMKV().decodeString(KEY_BOOKMARKS, "") }.getOrNull().orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<BrowserBookmark>>(raw) }.getOrDefault(emptyList())
    }

    fun isBookmarked(url: String): Boolean =
        url.isNotBlank() && getAll().any { it.url == url }

    fun toggle(url: String, title: String): Boolean {
        if (url.isBlank() || url == "about:blank") return false
        val current = getAll().toMutableList()
        val idx = current.indexOfFirst { it.url == url }
        val added =
            if (idx >= 0) {
                current.removeAt(idx)
                false
            } else {
                val cleanTitle = title.ifBlank { runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault(url) }
                current.add(0, BrowserBookmark(url = url, title = cleanTitle))
                true
            }
        runCatching { MMKV.defaultMMKV().encode(KEY_BOOKMARKS, json.encodeToString(current.take(100))) }
        return added
    }
}
