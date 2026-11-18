package com.junkfood.seal.ui.page.videolist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.outlined.Subtitles
import androidx.media3.common.C
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.junkfood.seal.App
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.FileUtil
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material3.RangeSlider
import com.junkfood.seal.util.LowRamOptimizer
import com.junkfood.seal.util.MediaProcessor
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val VIDEO_EXT = setOf("mp4", "mkv", "webm", "mov", "3gp")
private val AUDIO_EXT = setOf("m4a", "mp3", "opus", "ogg", "flac", "wav", "aac")

private val durationCache = ConcurrentHashMap<String, String>()

private enum class GalleryFilter(val label: String) {
    ALL("All"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    RECENT("Recent"),
}

private enum class GallerySort(val label: String) {
    NEWEST("Newest"),
    LARGEST("Largest"),
    NAME("Name A–Z"),
}

private data class TileMediaMeta(val thumb: Bitmap?, val durationText: String)

private suspend fun extractTileMeta(context: Context, f: File): TileMediaMeta =
    withContext(Dispatchers.IO) {
        val cache = LowRamOptimizer.getGalleryThumbCache(context)
        val key = f.absolutePath + ":" + f.lastModified()
        val cachedThumb = cache.get(key)
        val cachedDur = durationCache[key]
        if (cachedThumb != null && cachedDur != null) {
            return@withContext TileMediaMeta(cachedThumb, cachedDur)
        }
        val r = MediaMetadataRetriever()
        var bmp: Bitmap? = cachedThumb
        var durText = cachedDur.orEmpty()
        try {
            r.setDataSource(f.absolutePath)
            if (durText.isEmpty()) {
                val ms =
                    r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                if (ms > 0L) {
                    val sec = ms / 1000L
                    durText =
                        if (sec >= 3600) "%d:%02d:%02d".format(sec / 3600, sec % 3600 / 60, sec % 60)
                        else "%d:%02d".format(sec / 60, sec % 60)
                    durationCache[key] = durText
                }
            }
            if (bmp == null && f.extension.lowercase() in VIDEO_EXT) {
                bmp =
                    if (android.os.Build.VERSION.SDK_INT >= 27) {
                        r.getScaledFrameAtTime(
                            1_000_000,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            280,
                            280,
                        )
                    } else {
                        r.getFrameAtTime(1_000_000)?.let {
                            Bitmap.createScaledBitmap(it, 280, 280, true)
                        }
                    }
                bmp?.also { cache.put(key, it) }
            }
        } catch (_: Throwable) {
            // Ignore unreadable metadata
        } finally {
            runCatching { r.release() }
        }
        TileMediaMeta(bmp, durText)
    }

private fun formatFileSize(bytes: Long): String =
    when {
        bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        else -> "%.0f KB".format(bytes / 1024.0)
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current && isDarkTheme

    var files by remember { mutableStateOf<List<File>?>(null) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    var deletingFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(GalleryFilter.ALL) }
    var sort by remember { mutableStateOf(GallerySort.NEWEST) }
    var isGridView by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }

    suspend fun reload() {
        files =
            withContext(Dispatchers.IO) {
                listOf(App.videoDownloadDir, App.audioDownloadDir)
                    .distinct()
                    .filter { File(it).exists() }
                    .flatMap { File(it).walkTopDown().maxDepth(3).toList() }
                    .filter { it.isFile && it.extension.lowercase() in (VIDEO_EXT + AUDIO_EXT) }
                    .distinctBy { it.absolutePath }
                    .sortedByDescending { it.lastModified() }
            }
    }

    LaunchedEffect(Unit) { reload() }

    val backgroundColor =
        if (isGradientDark) GradientDarkColors.Background else MaterialTheme.colorScheme.background

    val allFiles = files.orEmpty()
    val nowMs = remember(allFiles) { System.currentTimeMillis() }
    val videoCount = remember(allFiles) { allFiles.count { it.extension.lowercase() in VIDEO_EXT } }
    val audioCount = remember(allFiles) { allFiles.count { it.extension.lowercase() in AUDIO_EXT } }
    val recentCount =
        remember(allFiles) { allFiles.count { nowMs - it.lastModified() <= 24L * 3600L * 1000L } }

    val displayedFiles =
        remember(allFiles, query, filter, sort) {
            allFiles
                .filter { f ->
                    val matchesFilter =
                        when (filter) {
                            GalleryFilter.ALL -> true
                            GalleryFilter.VIDEOS -> f.extension.lowercase() in VIDEO_EXT
                            GalleryFilter.AUDIO -> f.extension.lowercase() in AUDIO_EXT
                            GalleryFilter.RECENT ->
                                nowMs - f.lastModified() <= 24L * 3600L * 1000L
                        }
                    val matchesQuery =
                        query.isBlank() || f.name.contains(query.trim(), ignoreCase = true)
                    matchesFilter && matchesQuery
                }
                .let { list ->
                    when (sort) {
                        GallerySort.NEWEST -> list.sortedByDescending { it.lastModified() }
                        GallerySort.LARGEST -> list.sortedByDescending { it.length() }
                        GallerySort.NAME -> list.sortedBy { it.name.lowercase() }
                    }
                }
        }

    Scaffold(
        topBar = {
            if (selectedPaths.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selectedPaths.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedPaths = emptySet() }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val first =
                                    allFiles.firstOrNull { it.absolutePath in selectedPaths }
                                if (first != null) {
                                    FileUtil.createIntentForSharingFile(first.absolutePath)?.let {
                                        context.startActivity(
                                            Intent.createChooser(it, null)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share")
                        }
                        IconButton(
                            onClick = {
                                val targets = allFiles.filter { it.absolutePath in selectedPaths }
                                if (targets.isNotEmpty()) {
                                    deletingFiles = targets
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Gallery",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "$videoCount videos  •  $audioCount audio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = { BackButton(onNavigateBack) },
                    actions = {
                        IconButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val (cnt, freed) = LowRamOptimizer.cleanOrphanedPartFiles(
                                        listOf(App.videoDownloadDir, App.audioDownloadDir)
                                    )
                                    withContext(Dispatchers.Main) {
                                        val msg = if (cnt == 0) "No temp .part files found"
                                        else "Cleaned $cnt temp files (${formatFileSize(freed)})"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        if (cnt > 0) reload()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.CleaningServices, contentDescription = "Clean temp files")
                        }
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                imageVector =
                                    if (isGridView) Icons.Outlined.ViewAgenda
                                    else Icons.Outlined.GridView,
                                contentDescription = "Toggle layout",
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Outlined.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                            ) {
                                GallerySort.entries.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.label) },
                                        onClick = {
                                            sort = s
                                            showSortMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
                )
            }
        },
        containerColor = backgroundColor,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search gallery...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GalleryFilter.entries.forEach { f ->
                    val count =
                        when (f) {
                            GalleryFilter.ALL -> allFiles.size
                            GalleryFilter.VIDEOS -> videoCount
                            GalleryFilter.AUDIO -> audioCount
                            GalleryFilter.RECENT -> recentCount
                        }
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        shape = CircleShape,
                        label = { Text("${f.label} · $count") },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor =
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                            ),
                    )
                }
            }

            if (allFiles.isNotEmpty()) {
                val vBytes = remember(allFiles) {
                    allFiles.filter { it.extension.lowercase() in VIDEO_EXT }.sumOf { it.length() }
                }
                val aBytes = remember(allFiles) {
                    allFiles.filter { it.extension.lowercase() in AUDIO_EXT }.sumOf { it.length() }
                }
                val tot = (vBytes + aBytes).coerceAtLeast(1L)
                val vRatio = (vBytes.toFloat() / tot.toFloat()).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Videos ${formatFileSize(vBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Audio ${formatFileSize(aBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (vBytes > 0L) {
                            Box(
                                modifier = Modifier
                                    .weight(vRatio.coerceAtLeast(0.02f))
                                    .height(5.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        if (aBytes > 0L) {
                            Box(
                                modifier = Modifier
                                    .weight((1f - vRatio).coerceAtLeast(0.02f))
                                    .height(5.dp)
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                        }
                    }
                }
            }
            when {
                files == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                displayedFiles.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            )
                            Text(
                                text = "Your gallery is empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Downloaded videos and audio will appear here in a glossy grid.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                isGridView -> {
                    // Samsung Gallery-style tight 3-column glossy grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = displayedFiles,
                            key = { _, f -> f.absolutePath },
                        ) { index, f ->
                            val isSelected = f.absolutePath in selectedPaths
                            SamsungGlossyGridTile(
                                f = f,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectedPaths.isNotEmpty()) {
                                        selectedPaths =
                                            if (isSelected) selectedPaths - f.absolutePath
                                            else selectedPaths + f.absolutePath
                                    } else {
                                        previewIndex = index
                                    }
                                },
                                onLongClick = {
                                    selectedPaths =
                                        if (isSelected) selectedPaths - f.absolutePath
                                        else selectedPaths + f.absolutePath
                                },
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = displayedFiles,
                            key = { _, f -> f.absolutePath },
                        ) { index, f ->
                            val isSelected = f.absolutePath in selectedPaths
                            GalleryListRow(
                                f = f,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectedPaths.isNotEmpty()) {
                                        selectedPaths =
                                            if (isSelected) selectedPaths - f.absolutePath
                                            else selectedPaths + f.absolutePath
                                    } else {
                                        previewIndex = index
                                    }
                                },
                                onLongClick = {
                                    selectedPaths =
                                        if (isSelected) selectedPaths - f.absolutePath
                                        else selectedPaths + f.absolutePath
                                },
                                onShare = {
                                    FileUtil.createIntentForSharingFile(f.absolutePath)?.let {
                                        context.startActivity(
                                            Intent.createChooser(it, null)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                },
                                onDelete = { deletingFiles = listOf(f) },
                            )
                        }
                    }
                }
            }
        }
    }

    previewIndex?.let { idx ->
        if (idx in displayedFiles.indices) {
            FullscreenGalleryViewer(
                files = displayedFiles,
                initialIndex = idx,
                onDismiss = { previewIndex = null },
                onDelete = { target ->
                    previewIndex = null
                    deletingFiles = listOf(target)
                },
                onMediaCreated = { scope.launch { reload() } },
                onMoveToVault = { target ->
                    scope.launch(Dispatchers.IO) {
                        val vaultDir = File(App.videoDownloadDir, ".seal_vault").apply { mkdirs() }
                        val dest = File(vaultDir, target.name)
                        val moved = runCatching { target.renameTo(dest) }.getOrDefault(false)
                        withContext(Dispatchers.Main) {
                            previewIndex = null
                            Toast.makeText(
                                context,
                                if (moved) "Moved to Private Vault" else "Could not move file",
                                Toast.LENGTH_SHORT,
                            ).show()
                            if (moved) reload()
                        }
                    }
                },
            )
        }
    }

    if (deletingFiles.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { deletingFiles = emptyList() },
            title = {
                Text(
                    if (deletingFiles.size == 1) "Delete file?"
                    else "Delete ${deletingFiles.size} files?"
                )
            },
            text = {
                Text(
                    if (deletingFiles.size == 1) deletingFiles.first().name
                    else "This will permanently remove the selected media files from storage."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingFiles.forEach { runCatching { it.delete() } }
                        deletingFiles = emptyList()
                        selectedPaths = emptySet()
                        scope.launch { reload() }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingFiles = emptyList() }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SamsungGlossyGridTile(
    f: File,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val isAudio = f.extension.lowercase() in AUDIO_EXT
    var meta by remember(f.absolutePath) { mutableStateOf<TileMediaMeta?>(null) }
    LaunchedEffect(f.absolutePath) { meta = extractTileMeta(context, f) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue =
            when {
                isSelected -> 0.92f
                isPressed -> 0.95f
                else -> 1f
            },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tile_scale",
    )

    Box(
        modifier =
            Modifier.aspectRatio(1f)
                .scale(scale)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (isSelected) 2.5.dp else 0.6.dp,
                    color =
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                )
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
    ) {
        val t = meta?.thumb
        when {
            t != null -> {
                Image(
                    bitmap = t.asImageBitmap(),
                    contentDescription = f.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            isAudio -> {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                    )
                                )
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.AudioFile,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = f.nameWithoutExtension,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        // Glossy top specular sheen
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(28.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                        )
                    )
        )

        // Bottom frosted duration / format badge
        val badgeText =
            meta?.durationText?.takeIf { it.isNotBlank() } ?: f.extension.uppercase()
        Surface(
            color = Color.Black.copy(alpha = 0.62f),
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (!isAudio) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp),
                    )
                }
                Text(
                    text = badgeText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryListRow(
    f: File,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val isAudio = f.extension.lowercase() in AUDIO_EXT
    var meta by remember(f.absolutePath) { mutableStateOf<TileMediaMeta?>(null) }
    LaunchedEffect(f.absolutePath) { meta = extractTileMeta(context, f) }

    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp),
                        )
                    } else Modifier
                ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(96.dp, 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val t = meta?.thumb
                when {
                    t != null ->
                        Image(
                            t.asImageBitmap(),
                            null,
                            Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    isAudio -> Icon(Icons.Outlined.AudioFile, null)
                    else -> Icon(Icons.Outlined.PlayArrow, null)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    f.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    listOfNotNull(
                            formatFileSize(f.length()),
                            f.extension.uppercase(),
                            meta?.durationText?.takeIf { it.isNotBlank() },
                        )
                        .joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, "Share") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Delete") }
        }
    }
}

@AndroidXOptIn(UnstableApi::class)
@Composable
private fun FullscreenGalleryViewer(
    files: List<File>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onDelete: (File) -> Unit,
    onMoveToVault: (File) -> Unit,
    onMediaCreated: () -> Unit,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = initialIndex) { files.size }
    val pagerScope = rememberCoroutineScope()
    val currentIndex = pagerState.currentPage
    val currentFile = files[currentIndex]
    val isAudio = currentFile.extension.lowercase() in AUDIO_EXT
    val player = remember { LowRamOptimizer.createOptimizedExoPlayer(context) }

    var showControls by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showTrimDialog by remember { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }
    var gestureHudText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentFile.absolutePath) {
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(currentFile)))
        player.setPlaybackSpeed(speed)
        player.volume = if (isMuted) 0f else 1f
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            player.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        var seekHint by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(seekHint) {
            if (seekHint != null) {
                kotlinx.coroutines.delay(700L)
                seekHint = null
            }
        }
        LaunchedEffect(gestureHudText) {
            if (gestureHudText != null) {
                kotlinx.coroutines.delay(900L)
                gestureHudText = null
            }
        }
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val isLeft = change.position.x < size.width / 2f
                            if (isLeft) {
                                val win = (context as? Activity)?.window
                                if (win != null) {
                                    val lp = win.attributes
                                    val cur = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                    val next = (cur - dragAmount / 600f).coerceIn(0.05f, 1.0f)
                                    lp.screenBrightness = next
                                    win.attributes = lp
                                    gestureHudText = "Brightness: ${(next * 100).toInt()}%"
                                }
                            } else {
                                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val step = if (dragAmount < -12f) 1 else if (dragAmount > 12f) -1 else 0
                                if (step != 0) {
                                    val nextVol = (curVol + step).coerceIn(0, maxVol)
                                    am.setStreamVolume(AudioManager.STREAM_MUSIC, nextVol, 0)
                                    gestureHudText = "Volume: ${(nextVol * 100 / maxVol)}%"
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = { offset ->
                                val delta = if (offset.x < size.width / 2f) -10_000L else 10_000L
                                val target = (player.currentPosition + delta).coerceAtLeast(0L)
                                player.seekTo(target)
                                seekHint = if (delta < 0) "-10s" else "+10s"
                            },
                        )
                    }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                if (page != currentIndex) {
                    Box(Modifier.fillMaxSize().background(Color.Black))
                } else {
                    Box(Modifier.fillMaxSize())
                }
            }
            if (isAudio) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        Color.Black,
                                    )
                                )
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.size(120.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.AudioFile,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(56.dp),
                                )
                            }
                        }
                        Text(
                            text = currentFile.nameWithoutExtension,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        layoutParams = ViewGroup.LayoutParams(-1, -1)
                    }
                },
                update = { pv ->
                    pv.resizeMode =
                        if (isZoomed) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = if (isAudio) Modifier.size(1.dp) else Modifier.fillMaxSize(),
            )

            seekHint?.let { hint ->
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Text(
                        text = hint,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
            gestureHudText?.let { hud ->
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Text(
                        text = hud,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
            }
            // Glossy Top Bar Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                                )
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                        Text(
                            text = currentFile.nameWithoutExtension,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text =
                                "${currentIndex + 1}/${files.size}  •  ${formatFileSize(currentFile.length())}",
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color.White)
                    }
                    IconButton(onClick = { FileUtil.openFile(currentFile.absolutePath) {} }) {
                        Icon(
                            Icons.Outlined.OpenInNew,
                            contentDescription = "Open externally",
                            tint = Color.White,
                        )
                    }
                }
            }

            // Glossy Bottom Control Dock
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xCC141418),
                    modifier =
                        Modifier.navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.16f),
                                RoundedCornerShape(24.dp),
                            ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { pagerScope.launch { pagerState.animateScrollToPage(currentIndex - 1) } },
                            enabled = currentIndex > 0,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.NavigateBefore,
                                contentDescription = "Previous",
                                tint = if (currentIndex > 0) Color.White else Color.Gray,
                            )
                        }
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                player.volume = if (isMuted) 0f else 1f
                            }
                        ) {
                            Icon(
                                if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff
                                else Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White,
                            )
                        }
                        TextButton(
                            onClick = {
                                speed =
                                    when (speed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 1.0f
                                    }
                                player.setPlaybackSpeed(speed)
                            }
                        ) {
                            Text("${speed}x", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        if (!isAudio) {
                            IconButton(onClick = { isZoomed = !isZoomed }) {
                                Icon(
                                    Icons.Outlined.AspectRatio,
                                    contentDescription = "Fit/Zoom",
                                    tint = if (isZoomed) MaterialTheme.colorScheme.primary else Color.White,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                FileUtil.createIntentForSharingFile(currentFile.absolutePath)?.let {
                                    context.startActivity(
                                        Intent.createChooser(it, null)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = { showTracksDialog = true }) {
                            Icon(
                                Icons.Outlined.Subtitles,
                                contentDescription = "Audio & Subtitles",
                                tint = Color.White,
                            )
                        }
                        IconButton(onClick = { showTrimDialog = true }) {
                            Icon(
                                Icons.Outlined.ContentCut,
                                contentDescription = "Trim / Extract Audio",
                                tint = Color.White,
                            )
                        }
                        IconButton(onClick = { onMoveToVault(currentFile) }) {
                            Icon(
                                Icons.Outlined.VisibilityOff,
                                contentDescription = "Move to Vault",
                                tint = Color.White,
                            )
                        }
                        IconButton(onClick = { onDelete(currentFile) }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF6B6B),
                            )
                        }
                        IconButton(
                            onClick = { pagerScope.launch { pagerState.animateScrollToPage(currentIndex + 1) } },
                            enabled = currentIndex < files.lastIndex,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.NavigateNext,
                                contentDescription = "Next",
                                tint = if (currentIndex < files.lastIndex) Color.White else Color.Gray,
                            )
                        }
                    }
                }
            }
        }
    }



    if (showTracksDialog) {
        val audioLangs = remember(currentFile, showTracksDialog) {
            player.currentTracks.groups
                .filter { it.type == C.TRACK_TYPE_AUDIO }
                .flatMap { grp ->
                    (0 until grp.length).mapNotNull { idx -> grp.getTrackFormat(idx).language }
                }
                .distinct()
        }
        var subsDisabled by remember {
            mutableStateOf(player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        }
        AlertDialog(
            onDismissRequest = { showTracksDialog = false },
            title = { Text("Audio & Subtitles") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(
                        onClick = {
                            subsDisabled = !subsDisabled
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, subsDisabled)
                                .build()
                        }
                    ) {
                        Text(if (subsDisabled) "Enable Subtitles" else "Disable Subtitles")
                    }
                    if (audioLangs.isNotEmpty()) {
                        Text("Audio Tracks:", style = MaterialTheme.typography.labelMedium)
                        audioLangs.forEach { lang ->
                            TextButton(
                                onClick = {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .setPreferredAudioLanguage(lang)
                                        .build()
                                    showTracksDialog = false
                                }
                            ) { Text(lang.uppercase()) }
                        }
                    } else {
                        Text("Single default audio track", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTracksDialog = false }) { Text("Done") }
            },
        )
    }

    if (showTrimDialog) {
        val totalMs = remember(currentFile) { MediaProcessor.getDurationMs(currentFile).coerceAtLeast(1000L) }
        val totalSec = (totalMs / 1000f).coerceAtLeast(1f)
        var range by remember(currentFile) { mutableStateOf(0f..totalSec) }
        var busy by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!busy) showTrimDialog = false },
            title = { Text("Trim & Extract Audio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Range: ${range.start.toInt()}s – ${range.endInclusive.toInt()}s (of ${totalSec.toInt()}s)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    RangeSlider(
                        value = range,
                        onValueChange = { range = it },
                        valueRange = 0f..totalSec,
                    )
                    if (busy) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Processing...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && (range.endInclusive - range.start) >= 1f,
                    onClick = {
                        busy = true
                        pagerScope.launch(Dispatchers.IO) {
                            val outFile = File(
                                currentFile.parentFile,
                                "${currentFile.nameWithoutExtension}_trim_${range.start.toInt()}s.mp4",
                            )
                            val res = MediaProcessor.trimMedia(
                                input = currentFile,
                                output = outFile,
                                startMs = (range.start * 1000f).toLong(),
                                endMs = (range.endInclusive * 1000f).toLong(),
                            )
                            withContext(Dispatchers.Main) {
                                busy = false
                                showTrimDialog = false
                                Toast.makeText(
                                    context,
                                    if (res.isSuccess) "Saved: ${outFile.name}" else "Trim failed",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                if (res.isSuccess) onMediaCreated()
                            }
                        }
                    },
                ) { Text("Trim Clip") }
            },
            dismissButton = {
                Row {
                    if (!isAudio) {
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                busy = true
                                pagerScope.launch(Dispatchers.IO) {
                                    val outAudio = File(
                                        App.audioDownloadDir.let { File(it).apply { mkdirs() } },
                                        "${currentFile.nameWithoutExtension}.m4a",
                                    )
                                    val res = MediaProcessor.extractAudioTrack(currentFile, outAudio)
                                    withContext(Dispatchers.Main) {
                                        busy = false
                                        showTrimDialog = false
                                        Toast.makeText(
                                            context,
                                            if (res.isSuccess) "Extracted: ${outAudio.name}" else "Extract failed",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        if (res.isSuccess) onMediaCreated()
                                    }
                                }
                            },
                        ) { Text("Extract Audio") }
                    }
                    TextButton(enabled = !busy, onClick = { showTrimDialog = false }) { Text("Cancel") }
                }
            },
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Media Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name: ${currentFile.name}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Size: ${formatFileSize(currentFile.length())}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Format: ${currentFile.extension.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Modified: ${DateFormat.getDateTimeInstance().format(Date(currentFile.lastModified()))}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
            },
        )
    }
}
