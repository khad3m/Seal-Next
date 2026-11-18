package com.junkfood.seal.ui.page.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.common.HapticFeedback.slightHapticFeedback
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.common.ThemedIconColors
import com.junkfood.seal.ui.page.downloadv2.UiAction
import com.junkfood.seal.ui.page.downloadv2.configure.Config
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialog
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel.Action
import com.junkfood.seal.ui.page.downloadv2.configure.FormatPage
import com.junkfood.seal.ui.page.downloadv2.configure.PlaylistSelectionPage
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.FetchPreviewSheet
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.DatabaseUtil
import androidx.compose.material.icons.outlined.Wifi
import com.junkfood.seal.util.CONFIGURE
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.WifiQueueManager
import com.junkfood.seal.util.FileUtil
import java.io.File
import com.junkfood.seal.util.toFileSizeText
import com.junkfood.seal.util.getErrorReport
import com.junkfood.seal.util.makeToast
import com.junkfood.seal.util.matchUrlFromClipboard
import com.junkfood.seal.util.BATTERY_POPUP_HIDDEN
import com.junkfood.seal.util.BatteryUtil
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.updateLong
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun DownloadDetailsDialog(
    task: Task,
    state: Task.State,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showFilePathDialog by remember { mutableStateOf(false) }
    
    BackHandler {
        onDismiss()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.download_details),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = state.viewState.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            // Thumbnail Card
            state.videoInfo?.thumbnail?.let { thumbnailUrl ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Media Information Section
            Text(
                text = stringResource(R.string.media_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Grid Layout for Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: File Format and File Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.viewState.videoFormats?.firstOrNull()?.ext?.let { ext ->
                        if (ext.isNotBlank()) {
                            DetailCard(
                                icon = Icons.Outlined.VideoFile,
                                label = stringResource(R.string.file_format),
                                value = ext.uppercase(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    val fileSize = state.viewState.fileSizeApprox
                    if (fileSize > 0) {
                        DetailCard(
                            icon = Icons.Outlined.Storage,
                            label = stringResource(R.string.file_size),
                            value = fileSize.toFileSizeText(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Row 2: Creator and Platform
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.viewState.uploader.isNotBlank()) {
                        DetailCard(
                            icon = Icons.Outlined.Person,
                            label = stringResource(R.string.video_creator_label),
                            value = state.viewState.uploader,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (state.viewState.extractorKey.isNotBlank()) {
                        DetailCard(
                            icon = Icons.Outlined.Language,
                            label = stringResource(R.string.platform),
                            value = state.viewState.extractorKey,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Row 3: File Path and Download Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.downloadState is Task.DownloadState.Completed) {
                        state.downloadState.filePath?.let { path ->
                            DetailCard(
                                icon = Icons.Outlined.Folder,
                                label = stringResource(R.string.file_path),
                                value = path,
                                modifier = Modifier.weight(1f),
                                onClick = { showFilePathDialog = true }
                            )
                        }
                    }
                    
                    DetailCard(
                        icon = Icons.Outlined.CalendarToday,
                        label = stringResource(R.string.download_date),
                        value = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(task.timeCreated)),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Source URL Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(state.viewState.url))
                            context.makeToast(R.string.link_copied)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.source_url),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = state.viewState.url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // File Path Dialog
    if (showFilePathDialog && state.downloadState is Task.DownloadState.Completed) {
        state.downloadState.filePath?.let { path ->
            AlertDialog(
                onDismissRequest = { showFilePathDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.file_path),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    SelectionContainer {
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFilePathDialog = false }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }
    }
}

/**
 * Spotify-style swipeable download-details sheet. Wraps a [HorizontalPager] over
 * [downloadInfoList] so the user can swipe left/right to move between the details of
 * adjacent recent-download cards without closing and reopening the sheet. When the list
 * has only one item, the pager still mounts but swiping is disabled (no-op, no crash).
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RecentDownloadDetailsDialog(
    downloadInfoList: List<DownloadedVideoInfo>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val safeInitialIndex = initialIndex.coerceIn(0, (downloadInfoList.size - 1).coerceAtLeast(0))
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = safeInitialIndex
    ) { downloadInfoList.size }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current

    // BUG FIX — continuous up/down flicker after releasing a swipe-up gesture:
    //
    // The sheet's own drag-to-expand/dismiss gesture and each page's inner verticalScroll
    // both live on the same vertical axis. Once a swipe-up reaches the top/bottom of the
    // scrollable content, any velocity/delta the inner scroll can't consume is normally
    // handed UP to the ModalBottomSheet's drag handler (that's how nested scroll works by
    // default: children consume first, parents get the "pre" pass and any leftovers).
    // Because the sheet height is a fixed 85% of the screen, its resting/expanded anchor sits
    // very close to the content's natural bounds on most devices — so that tiny leftover
    // delta/velocity is just enough to nudge the sheet, which nudges the scroll state back,
    // which nudges the sheet again... an unstable feedback loop that never settles. That is
    // the repeating up/down flicker: it does not stop on its own once triggered by a
    // swipe-and-release.
    //
    // Fix: swallow all scroll deltas AND fling velocity in a NestedScrollConnection placed
    // between the pager/content and the ModalBottomSheet, so nothing above this point ever
    // sees "leftover" scroll to react to. The sheet still drags normally from its own drag
    // handle / empty areas — this only isolates the content's internal scrolling.
    val swallowNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = available

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                available
        }
    }

    BackHandler {
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(swallowNestedScroll),
        ) {
            // Page indicator dots — only shown when there's more than one item to swipe between.
            if (downloadInfoList.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(downloadInfoList.size) { dotIndex ->
                        val isSelected = dotIndex == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }

            // IMPORTANT: use a FIXED height here (not heightIn(max=...)). Each page's intrinsic
            // content height differs (some items have extra detail rows, longer URLs, etc.).
            // With only a max-height constraint, HorizontalPager re-measures its own height to
            // match whichever page is being settled on during a drag, and that continuous
            // height renegotiation while the inner verticalScroll is also tracking its own
            // bounds is what produced the up/down flicker when scrolling a page to its end.
            // Giving every page the exact same fixed height removes that feedback loop entirely.
            val pagerHeight = (configuration.screenHeightDp * 0.85f).dp
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pagerHeight),
                // Disable swipe gestures entirely when there's nothing to swipe to — keeps a
                // single-item list on its one and only details page with no dead gesture area.
                userScrollEnabled = downloadInfoList.size > 1,
            ) { page ->
                RecentDownloadDetailsContent(
                    downloadInfo = downloadInfoList[page],
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun RecentDownloadDetailsContent(
    downloadInfo: DownloadedVideoInfo,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showFilePathDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.download_details),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = downloadInfo.videoTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            // Thumbnail Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                AsyncImage(
                    model = downloadInfo.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Media Information Section
            Text(
                text = stringResource(R.string.media_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Grid Layout for Details
            val file = java.io.File(downloadInfo.videoPath)
            val fileExtension = downloadInfo.videoPath.substringAfterLast(".", "")
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: File Format and File Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (fileExtension.isNotEmpty()) {
                        DetailCard(
                            icon = Icons.Outlined.VideoFile,
                            label = stringResource(R.string.file_format),
                            value = fileExtension.uppercase(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (file.exists()) {
                        val fileSize = file.length()
                        DetailCard(
                            icon = Icons.Outlined.Storage,
                            label = stringResource(R.string.file_size),
                            value = fileSize.toFileSizeText(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Row 2: Resolution and Platform
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Extract resolution from video file.
                    // Uses produceState + Dispatchers.IO so the blocking MediaMetadataRetriever
                    // call never runs on the main thread (avoids ANR / frame drops on large
                    // files or slow storage). try/finally guarantees release() even if
                    // setDataSource() throws (minSdk=24, AutoCloseable added in API 29).
                    val resolution by produceState(initialValue = "N/A", key1 = downloadInfo.videoPath) {
                        value = withContext(Dispatchers.IO) {
                            val retriever = android.media.MediaMetadataRetriever()
                            try {
                                if (file.exists()) {
                                    retriever.setDataSource(downloadInfo.videoPath)
                                    val width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                                    val height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                                    if (width != null && height != null) "${width}x${height}" else "N/A"
                                } else "N/A"
                            } catch (e: Exception) {
                                "N/A"
                            } finally {
                                retriever.release()
                            }
                        }
                    }
                    
                    if (resolution != "N/A") {
                        DetailCard(
                            icon = Icons.Outlined.HighQuality,
                            label = stringResource(R.string.resolution),
                            value = resolution,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    DetailCard(
                        icon = Icons.Outlined.Language,
                        label = stringResource(R.string.platform),
                        value = downloadInfo.extractor,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Row 3: File Path and Download Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailCard(
                        icon = Icons.Outlined.Folder,
                        label = stringResource(R.string.file_path),
                        value = downloadInfo.videoPath,
                        modifier = Modifier.weight(1f),
                        onClick = { showFilePathDialog = true }
                    )
                    
                    val downloadDate = if (file.exists()) {
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(file.lastModified()))
                    } else {
                        "N/A"
                    }
                    DetailCard(
                        icon = Icons.Outlined.CalendarToday,
                        label = stringResource(R.string.download_date),
                        value = downloadDate,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: Download Time and Average Speed
                if (downloadInfo.downloadTimeMillis > 0L || downloadInfo.averageSpeedBytesPerSec > 0L) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (downloadInfo.downloadTimeMillis > 0L) {
                            DetailCard(
                                icon = Icons.Outlined.Timer,
                                label = stringResource(R.string.download_time),
                                value = formatDownloadTime(downloadInfo.downloadTimeMillis),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (downloadInfo.averageSpeedBytesPerSec > 0L) {
                            DetailCard(
                                icon = Icons.Outlined.Speed,
                                label = stringResource(R.string.average_speed),
                                value = formatAverageSpeed(downloadInfo.averageSpeedBytesPerSec),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Source URL Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(downloadInfo.videoUrl))
                            context.makeToast(R.string.link_copied)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.source_url),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = downloadInfo.videoUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
    }

    // File Path Dialog
    if (showFilePathDialog) {
        AlertDialog(
            onDismissRequest = { showFilePathDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.file_path),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                SelectionContainer {
                    Text(
                        text = downloadInfo.videoPath,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilePathDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

internal fun formatDownloadTime(millis: Long): String {
    val totalSeconds = millis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

internal fun formatAverageSpeed(bytesPerSec: Long): String {
    val mb = 1024L * 1024L
    val kb = 1024L
    return when {
        bytesPerSec >= mb -> "%.1f MB/s".format(bytesPerSec.toDouble() / mb)
        bytesPerSec >= kb -> "${bytesPerSec / kb} KB/s"
        else -> "$bytesPerSec B/s"
    }
}

@Composable
private fun DetailCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else Modifier
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Returns a numeric sort priority for a [Task.DownloadState] so that the
 * Recent Downloads list always shows items in the order:
 *
 *   Running                (0)    →  actively downloading NOW
 *   FetchingInfo           (1)    →  actively fetching metadata NOW
 *   ReadyWithInfo          (2)    →  info fetched, waiting for a download slot
 *   Idle                   (3)    →  just queued, nothing started yet
 *   Paused                 (4)    →  user-paused
 *   Canceled               (5)    →  user-canceled
 *   Error                  (6)    →  failed
 *   Completed              (7)    →  transition state before DB flush
 *
 * Lower number = shown closer to the top of the list.
 */
internal fun downloadStateSortPriority(state: Task.DownloadState): Int = when (state) {
    is Task.DownloadState.Running       -> 0  // actively downloading right now
    is Task.DownloadState.FetchingInfo  -> 1  // actively fetching metadata right now
    Task.DownloadState.ReadyWithInfo    -> 2  // info fetched, waiting for a download slot — more advanced than Idle
    Task.DownloadState.Idle             -> 3  // just queued, nothing started yet
    is Task.DownloadState.Paused        -> 4  // user-paused
    is Task.DownloadState.Canceled      -> 5  // user-canceled
    is Task.DownloadState.Error         -> 6  // failed
    is Task.DownloadState.Completed     -> 7  // done, transitioning to DB section
}

/**
 * Animated glowing "+" text with continuously cycling gradient colors
 * and a pulsing glow effect that matches the app theme.
 */
