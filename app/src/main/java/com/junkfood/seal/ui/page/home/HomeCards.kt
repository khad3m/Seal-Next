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
fun URLInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onDownloadClick: () -> Unit,
    onPasteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    val fullPlaceholder = stringResource(R.string.enter_url_to_download)

    // Typewriter animation: reveal characters one by one
    var displayedLength by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        displayedLength = 0
        for (i in 1..fullPlaceholder.length) {
            delay(50L)
            displayedLength = i
        }
    }

    // Gradient animation for the placeholder text
    val infiniteTransition = rememberInfiniteTransition(label = "placeholderGradient")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "placeholderShift"
    )

    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
    )

    val gradientBrush = Brush.linearGradient(
        colors = gradientColors,
        start = Offset(gradientShift, 0f),
        end = Offset(gradientShift + 400f, 0f),
        tileMode = TileMode.Mirror
    )
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        placeholder = {
            Text(
                text = fullPlaceholder.take(displayedLength),
                style = MaterialTheme.typography.bodyLarge.merge(
                    TextStyle(brush = gradientBrush)
                )
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(32.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDownloadClick() }),
        trailingIcon = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (value.isEmpty()) {
                    IconButton(onClick = onPasteClick) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = "Paste",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                FilledIconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 4.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isGradientDark && isDarkTheme) {
                            GradientDarkColors.GradientPrimaryStart
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = stringResource(R.string.download),
                        tint = Color.White
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isGradientDark && isDarkTheme) {
                GradientDarkColors.GradientPrimaryStart
            } else {
                MaterialTheme.colorScheme.primary
            },
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun RecentDownloadCard(
    downloadInfo: DownloadedVideoInfo,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onShowDetails: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit = {},
    refreshKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    var showMenu by remember { mutableStateOf(false) }
    // Use produceState so fileExists is re-checked on every recomposition trigger (refreshKey
    // changes on ON_RESUME), and also whenever the video path itself changes.
    val fileExists by produceState(initialValue = java.io.File(downloadInfo.videoPath).exists(), key1 = downloadInfo.videoPath, key2 = refreshKey) {
        value = java.io.File(downloadInfo.videoPath).exists()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (fileExists) 1f else 0.55f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGradientDark && isDarkTheme) {
                GradientDarkColors.SurfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = downloadInfo.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = downloadInfo.videoTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fileExists) {
                        Text(
                            text = stringResource(R.string.completed),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isGradientDark && isDarkTheme) {
                                Color(0xFF4ADE80)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.BrokenImage,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.file_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // More button with dropdown menu
            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.details)) },
                        onClick = {
                            onShowDetails()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = {
                            onShare()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy_link)) },
                        onClick = {
                            onCopyLink()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.hide)) },
                        onClick = {
                            onHide()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveDownloadCard(
    task: Task,
    state: Task.State,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    var showMenu by remember { mutableStateOf(false) }
    
    val downloadState = state.downloadState
    val progress = when (downloadState) {
        is Task.DownloadState.Running -> downloadState.progress
        is Task.DownloadState.Paused -> downloadState.progress ?: -1f
        is Task.DownloadState.Canceled -> downloadState.progress ?: -1f
        else -> 0f
    }
    
    // Parse progress text to determine download phase
    val progressText = if (downloadState is Task.DownloadState.Running) downloadState.progressText else ""
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Track download state: format info -> video download -> audio download -> merging
    var hasSeenFormatInfo by remember { mutableStateOf(false) }
    var hasSeenVideoComplete by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf("downloading") }
    
    // Determine phase based on progressText patterns
    // NOTE: DownloaderV2 strips the "[download] " prefix before storing progressText,
    // so download progress lines look like "45.3% of 10.00MiB at 2.50MiB/s ETA 00:03"
    // or "100% of 10.00MiB in 00:04". We detect them by looking for the % sign with digits.
    val downloadPhase = when {
        // Merging phase — [Merger] prefix is NOT stripped
        progressText.contains("[Merger]", ignoreCase = true) ||
        progressText.contains("Merging formats", ignoreCase = true) -> {
            currentPhase = "merging"
            hasSeenVideoComplete = false
            hasSeenFormatInfo = false
            "merging"
        }
        // Format info line — [info] prefix is NOT stripped
        progressText.contains("[info]", ignoreCase = true) && progressText.contains("format", ignoreCase = true) -> {
            hasSeenFormatInfo = true
            hasSeenVideoComplete = false
            currentPhase = "downloading"
            "downloading"
        }
        // Download progress lines — "[download]" prefix stripped; match % pattern instead
        progressText.matches(Regex("""^\d+(\.\d+)?%.*""")) || progressText.contains("% of ") -> {
            when {
                // 100% completion — video stream done, audio stream is next
                (progressText.startsWith("100%") || progressText.contains("100% of ")) && !hasSeenVideoComplete -> {
                    hasSeenVideoComplete = true
                    currentPhase = "video"
                    "video"
                }
                // After video complete, any download progress is the audio stream
                hasSeenVideoComplete -> {
                    currentPhase = "audio"
                    "audio"
                }
                // Before any 100% seen, first stream is always video
                hasSeenFormatInfo -> {
                    currentPhase = "video"
                    "video"
                }
                else -> {
                    currentPhase = "downloading"
                    "downloading"
                }
            }
        }
        // Post-download file operations — maintain current phase
        progressText.contains("Deleting original file", ignoreCase = true) ||
        progressText.contains("[Metadata]", ignoreCase = true) ||
        progressText.contains("[MoveFiles]", ignoreCase = true) -> {
            currentPhase
        }
        // yt-dlp re-outputs [youtube] / "Downloading webpage" lines between streams.
        // In Running state we are always downloading (FetchingInfo state handles the fetch phase).
        progressText.contains("[youtube]", ignoreCase = true) ||
        progressText.contains("Downloading webpage", ignoreCase = true) ||
        progressText.contains("Downloading player", ignoreCase = true) -> {
            if (hasSeenVideoComplete) {
                // yt-dlp is initializing the second (audio) stream
                currentPhase = "audio"
                "audio"
            } else {
                currentPhase  // Maintain current phase — never show "fetching" while running
            }
        }
        else -> currentPhase
    }
    
    val statusText = when (downloadState) {
        is Task.DownloadState.Running -> {
            val pct = if (progress >= 0) " ${(progress * 100).toInt()}%" else ""
            when (downloadPhase) {
                "merging" -> stringResource(R.string.status_merging)
                "video"   -> "Downloading video...$pct"
                "audio"   -> "Downloading audio...$pct"
                "fetching" -> stringResource(R.string.fetching_info)
                else -> if (progress >= 0) "Downloading... ${(progress * 100).toInt()}%"
                        else stringResource(R.string.status_downloading)
            }
        }
        is Task.DownloadState.Paused -> if (progress >= 0) stringResource(R.string.status_paused) + " ${(progress * 100).toInt()}%" else stringResource(R.string.status_paused)
        is Task.DownloadState.Canceled -> stringResource(R.string.status_canceled)
        is Task.DownloadState.Error -> stringResource(R.string.download_error)
        is Task.DownloadState.Completed -> stringResource(R.string.completed) + " 100%"
        is Task.DownloadState.FetchingInfo -> stringResource(R.string.fetching_info)
        // Idle = waiting for a download slot to open; ReadyWithInfo = info fetched, waiting to start
        Task.DownloadState.Idle,
        Task.DownloadState.ReadyWithInfo -> stringResource(R.string.queue_status)
        else -> ""
    }
    
    val statusColor = when (downloadState) {
        is Task.DownloadState.Running -> if (isGradientDark && isDarkTheme) {
            GradientDarkColors.GradientPrimaryStart
        } else {
            MaterialTheme.colorScheme.primary
        }
        is Task.DownloadState.Paused -> Color(0xFFFBBF24)
        is Task.DownloadState.Canceled -> Color(0xFFEF4444)
        is Task.DownloadState.Error -> Color(0xFFEF4444)
        is Task.DownloadState.Completed -> Color(0xFF4ADE80)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Parse speed and ETA from yt-dlp progressText.
    // Example line: "45.3% of 10.00MiB at 2.50MiB/s ETA 00:03"
    val speedEtaText = if (downloadState is Task.DownloadState.Running && progressText.isNotEmpty()) {
        val speed = Regex("""at\s+([\d.]+\s*\S+/s)""").find(progressText)?.groupValues?.get(1)
        val eta = Regex("""ETA\s+(\d+:\d+)""").find(progressText)?.groupValues?.get(1)
        when {
            speed != null && eta != null -> "$speed  •  ETA $eta"
            speed != null -> speed
            eta != null -> "ETA $eta"
            else -> null
        }
    } else null
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGradientDark && isDarkTheme) {
                GradientDarkColors.SurfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                state.videoInfo?.thumbnail?.let { thumbnailUrl ->
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = state.viewState.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Show Queue badge for Idle or ReadyWithInfo tasks
                        if (downloadState is Task.DownloadState.Idle || downloadState is Task.DownloadState.ReadyWithInfo) {
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isGradientDark && isDarkTheme) {
                                    GradientDarkColors.GradientSecondaryStart.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.queue_status),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isGradientDark && isDarkTheme) {
                                        GradientDarkColors.GradientSecondaryEnd
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Speed + ETA line — only shown during active download
                    if (speedEtaText != null) {
                        Text(
                            text = speedEtaText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Pause/Resume action button
                if (downloadState is Task.DownloadState.Running) {
                    IconButton(
                        onClick = { onAction(UiAction.Pause) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Pause,
                            contentDescription = stringResource(R.string.pause),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                if (downloadState is Task.DownloadState.Paused) {
                    IconButton(
                        onClick = { onAction(UiAction.Resume) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(R.string.resume),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // More button with dropdown menu
                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        val downloadState = state.downloadState
                        
                        // Pause option for running downloads
                        if (downloadState is Task.DownloadState.Running) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pause)) },
                                onClick = {
                                    onAction(UiAction.Pause)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Pause,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                        
                        // Resume option for paused downloads
                        if (downloadState is Task.DownloadState.Paused) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.resume)) },
                                onClick = {
                                    onAction(UiAction.Resume)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                        
                        // Retry option for canceled or failed downloads
                        if (downloadState is Task.DownloadState.Canceled || downloadState is Task.DownloadState.Error) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.retry)) },
                                onClick = {
                                    onAction(UiAction.Retry)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                        
                        // Cancel option for running/fetching/paused/queued downloads.
                        // Idle and ReadyWithInfo are queued states — DownloaderV2.cancelImpl()
                        // handles them correctly but the UI must expose the action.
                        if (downloadState is Task.DownloadState.Running ||
                            downloadState is Task.DownloadState.FetchingInfo ||
                            downloadState is Task.DownloadState.Paused ||
                            downloadState == Task.DownloadState.Idle ||
                            downloadState == Task.DownloadState.ReadyWithInfo) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cancel)) },
                                onClick = {
                                    onAction(UiAction.Cancel)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Cancel,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            )
                        }
                        
                        // Copy link option
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.copy_link)) },
                            onClick = {
                                onAction(UiAction.CopyVideoURL)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        )
                        
                        // Details option (only for completed downloads)
                        if (downloadState is Task.DownloadState.Completed) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.details)) },
                                onClick = {
                                    onAction(UiAction.ShowDetails)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            )
                        }
                        
                        // Delete option
                        if (downloadState is Task.DownloadState.Completed || downloadState is Task.DownloadState.Error || downloadState is Task.DownloadState.Canceled) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    onAction(UiAction.Delete)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            // Progress bar for active and paused downloads
            if (downloadState is Task.DownloadState.Running || downloadState is Task.DownloadState.Paused) {
                val barColor = when (downloadState) {
                    is Task.DownloadState.Paused -> Color(0xFFFBBF24)
                    else -> if (isGradientDark && isDarkTheme) GradientDarkColors.GradientPrimaryStart
                            else MaterialTheme.colorScheme.primary
                }
                if (progress >= 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun AnimatedGlowingPlus() {
    val infiniteTransition = rememberInfiniteTransition(label = "plusGlow")

    // Animate the gradient offset to make colors flow continuously
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientShift"
    )

    // Animate glow intensity (pulsing alpha for the shadow)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
    )

    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.6f)

    val gradientBrush = Brush.linearGradient(
        colors = gradientColors,
        start = Offset(gradientShift, 0f),
        end = Offset(gradientShift + 500f, 500f),
        tileMode = TileMode.Mirror
    )

    Text(
        text = "+",
        style = MaterialTheme.typography.displayMedium.merge(
            TextStyle(
                brush = gradientBrush,
                shadow = Shadow(
                    color = glowColor,
                    offset = Offset.Zero,
                    blurRadius = 16f * glowAlpha
                )
            )
        ),
        fontWeight = FontWeight.Bold
    )
}

/**
 * Quick-access row for the 4 More Tools (Batch URL Import, Thumbnail Download, Video Info
 * Download, Comment Download), placed between the "Seal+" branding and the URL input field.
 *
 * Icon-only by design — no labels/section header — so it reads as a native strip of shortcuts
 * baked into the home screen rather than a bolted-on section. Colors reuse the same
 * primary/secondary/tertiary role-color language already used for icons in the nav drawer and
 * the More Tools page ([ThemedIconColors]), so the row feels like part of the existing UI
 * instead of a new visual style.
 */
private data class QuickTool(
    val icon: ImageVector,
    val labelRes: Int,
    val tint: @Composable () -> Color,
    val onClick: () -> Unit,
)

@Composable
internal fun QuickToolsRow(
    onThumbnailDownload: () -> Unit,
    onVideoInfoDownload: () -> Unit,
    onCommentDownload: () -> Unit,
    onBatchUrlImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tools = remember(
        onBatchUrlImport,
        onThumbnailDownload,
        onVideoInfoDownload,
        onCommentDownload,
    ) {
        listOf(
            QuickTool(
                icon = Icons.Outlined.PlaylistAdd,
                labelRes = R.string.batch_url_import,
                tint = { ThemedIconColors.primary },
                onClick = onBatchUrlImport,
            ),
            QuickTool(
                icon = Icons.Outlined.Image,
                labelRes = R.string.thumbnail_download,
                tint = { ThemedIconColors.secondary },
                onClick = onThumbnailDownload,
            ),
            QuickTool(
                icon = Icons.Outlined.Description,
                labelRes = R.string.video_info_download,
                tint = { ThemedIconColors.tertiary },
                onClick = onVideoInfoDownload,
            ),
            QuickTool(
                icon = Icons.Outlined.Chat,
                labelRes = R.string.comment_download,
                tint = { ThemedIconColors.primary },
                onClick = onCommentDownload,
            ),
        )
    }

    // Gentle entrance animation (fade + rise), matching the staggered-card language used
    // elsewhere in the app (e.g. MoreToolsPage's ToolCard) instead of appearing abruptly.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tools.forEachIndexed { index, tool ->
            QuickToolIcon(
                tool = tool,
                visible = visible,
                animationDelayMs = index * 60,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickToolIcon(
    tool: QuickTool,
    visible: Boolean,
    animationDelayMs: Int,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 380,
            delayMillis = animationDelayMs,
            easing = FastOutSlowInEasing,
        ),
        label = "quickTool_alpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 12.dp,
        animationSpec = tween(
            durationMillis = 380,
            delayMillis = animationDelayMs,
            easing = FastOutSlowInEasing,
        ),
        label = "quickTool_offset",
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "quickTool_scale",
    )

    val tint = tool.tint()

    Box(
        modifier = Modifier
            .graphicsLayer(alpha = alpha)
            .offset(y = offsetY)
            .scale(scale)
            // Sized to match the same icon-badge scale used for icons in the nav drawer /
            // More Tools page (44dp container / 22dp glyph) instead of the earlier oversized
            // 56dp/26dp, so this row reads proportionate on every screen size from small
            // phones to tablets rather than dominating the space between the title and search
            // bar.
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.slightHapticFeedback()
                    tool.onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = tool.icon,
            contentDescription = stringResource(tool.labelRes),
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}


@Composable
internal fun MediaHubStrip(
    onOpenYouTubePlayer: () -> Unit,
    onOpenSocialViewer: () -> Unit,
    onOpenGallery: () -> Unit,
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current && isDarkTheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MediaHubCard(
            title = "YouTube",
            subtitle = "Player",
            icon = Icons.Outlined.PlayCircle,
            tint = MaterialTheme.colorScheme.primary,
            isGradientDark = isGradientDark,
            onClick = onOpenYouTubePlayer,
            modifier = Modifier.weight(1f),
        )
        MediaHubCard(
            title = "Social",
            subtitle = "IG · TT · FB",
            icon = Icons.Outlined.Public,
            tint = MaterialTheme.colorScheme.secondary,
            isGradientDark = isGradientDark,
            onClick = onOpenSocialViewer,
            modifier = Modifier.weight(1f),
        )
        MediaHubCard(
            title = "Gallery",
            subtitle = "Media",
            icon = Icons.Outlined.VideoLibrary,
            tint = MaterialTheme.colorScheme.tertiary,
            isGradientDark = isGradientDark,
            onClick = onOpenGallery,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MediaHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    isGradientDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "media_hub_scale",
    )
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 3.dp,
        color = if (isGradientDark) {
            GradientDarkColors.GlassSurface.copy(alpha = 0.06f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = modifier
            .scale(scale)
            .then(
                if (isGradientDark) {
                    Modifier.border(
                        1.dp,
                        GradientDarkColors.GlassWhiteBorder,
                        RoundedCornerShape(18.dp),
                    )
                } else Modifier
            ),
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isGradientDark) 0.08f else 0.35f),
                            Color.Transparent,
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(19.dp),
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
