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
fun NewHomePage(
    modifier: Modifier = Modifier,
    onMenuOpen: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToBatchUrlImport: () -> Unit = {},
    onNavigateToVideoInfoDownload: () -> Unit = {},
    onNavigateToThumbnailDownload: () -> Unit = {},
    onNavigateToCommentDownload: () -> Unit = {},
    onNavigateToStream: () -> Unit = {},
    onNavigateToSocialViewer: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    dialogViewModel: DownloadDialogViewModel,
    downloader: DownloaderV2 = koinInject(),
) {
    val view = LocalView.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    
    var showExitDialog by remember { mutableStateOf(false) }
    var urlText by remember { mutableStateOf("") }
    // Premium fetch sheet (bug #1): URL currently shown in the auto-fetch preview sheet,
    // or null when the sheet is closed.
    var fetchSheetUrl by remember { mutableStateOf<String?>(null) }
    var duplicateUrlPending by remember { mutableStateOf<String?>(null) }
    var duplicateTitle by remember { mutableStateOf<String?>(null) }
    val wifiQueue by WifiQueueManager.queuedUrls.collectAsStateWithLifecycle()
    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }
    var dismissedClipboardUrl by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Pre-fill URL from share intent and auto-open the download dialog.
    // The original code only filled the text field without opening the dialog, causing the
    // "nothing appears" bug when a URL was shared to the app — the URL silently appeared in
    // the input box with no feedback and no dialog. Match the behavior of DownloadPageV2
    // which calls postAction(Action.ShowSheet(...)) on share.
    val sharedUrl by dialogViewModel.sharedUrlFlow.collectAsState()
    LaunchedEffect(sharedUrl) {
        if (sharedUrl.isNotBlank()) {
            urlText = sharedUrl
            dialogViewModel.consumeSharedUrl()
            // Auto-open the dialog with the shared URL so the user immediately sees it,
            // rather than having to find and tap the text field.
            dialogViewModel.postAction(Action.ShowSheet(listOf(sharedUrl)))
        }
    }
    
    // Get lifecycle owner
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    // State to track lifecycle and force refresh
    var lifecycleRefreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(lifecycleRefreshTrigger) {
        val clip = clipboardManager.getText()?.text
        val matched = clip?.let { context.matchUrlFromClipboard(it) }
        if (!matched.isNullOrBlank() && matched != dismissedClipboardUrl && matched != urlText) {
            detectedClipboardUrl = matched
        } else {
            detectedClipboardUrl = null
        }
    }
    
    // Monitor lifecycle events to trigger refresh when screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleRefreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Permission states
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
    var permissionsChecked by remember { mutableStateOf(false) }
    
    // Check notification permission
    val hasNotificationPermission = remember(lifecycleRefreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Not needed below Android 13
        }
    }
    
    // Check battery optimization
    // NOTE: no cooldown/dismissal flag at all — this reminder should show every single time the
    // app is opened for as long as battery optimization is still not disabled, since disabling
    // it is required for reliable background downloads. There's nothing to remember between
    // launches: shouldPromptBatteryDialog() always reflects the live, real-time system state.
    //
    // This is a directly settable mutableStateOf (not a remember(key) derived value) because we
    // need to refresh it from TWO independent triggers: (1) lifecycleRefreshTrigger on every
    // ON_RESUME, and (2) the battery settings activity-result callback below. #2 exists because
    // on some OEM ROMs, the OS does not commit/propagate the new battery-optimization
    // whitelist state instantly — there can be a short delay between the user picking "No
    // restrictions" in the settings screen and PowerManager.isIgnoringBatteryOptimizations()
    // actually reflecting it. Reading it at the exact instant ON_RESUME fires (right as the
    // user presses back) can race that propagation and read the stale "still restricted" value,
    // which is exactly why the dialog kept reappearing even after the user had correctly fixed
    // the setting. Re-checking again after a short delay closes that race.
    var isBatteryOptimizationDisabled by remember {
        mutableStateOf(BatteryUtil.isIgnoringBatteryOptimizations(context))
    }
    val isBatteryPromptSnoozed = remember {
        val until = runCatching { com.tencent.mmkv.MMKV.defaultMMKV().decodeLong("battery_prompt_snooze_until", 0L) }.getOrDefault(0L)
        System.currentTimeMillis() < until
    }
    val shouldPromptBatteryDialog = { !isBatteryOptimizationDisabled }

    LaunchedEffect(lifecycleRefreshTrigger) {
        isBatteryOptimizationDisabled = BatteryUtil.isIgnoringBatteryOptimizations(context)
    }
    
    // Notification permission launcher - tries system permission first
    // Notification settings launcher - opens app notification settings
    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Permission state will be checked on resume */ }
    
    // Battery optimization launcher. The activity-result callback fires right when the user
    // returns from the OS battery settings screen (whether via back press or completing a
    // system dialog) — we re-check immediately AND again after a short delay (see the NOTE
    // above the isBatteryOptimizationDisabled declaration) to avoid racing a delayed OS-side
    // whitelist update on some OEM ROMs.
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isBatteryOptimizationDisabled = BatteryUtil.isIgnoringBatteryOptimizations(context)
        scope.launch {
            delay(500L)
            isBatteryOptimizationDisabled = BatteryUtil.isIgnoringBatteryOptimizations(context)
        }
    }

    
    // Check permissions on first load
    LaunchedEffect(Unit) {
        if (!permissionsChecked) {
            permissionsChecked = true
            if (!hasNotificationPermission) {
                showNotificationPermissionDialog = true
            } else if (shouldPromptBatteryDialog()) {
                showBatteryOptimizationDialog = true
            }
        }
    }
    
    // Monitor permission state changes to show next dialog when user returns from settings
    LaunchedEffect(hasNotificationPermission, isBatteryOptimizationDisabled) {
        if (permissionsChecked) {
            // If notification dialog was shown and is now dismissed
            if (!showNotificationPermissionDialog && hasNotificationPermission && shouldPromptBatteryDialog()) {
                // Show battery optimization dialog after notification permission is granted
                showBatteryOptimizationDialog = true
            }
        }
    }

    // Re-prompt on every app resume, even when isBatteryOptimizationDisabled's VALUE hasn't
    // changed. NOTE: the effect above only fires when hasNotificationPermission or
    // isBatteryOptimizationDisabled actually CHANGE VALUE — if battery optimization was already
    // restricted before this resume and is STILL restricted after it (the common case: the user
    // dismissed the dialog without touching the setting), the boolean is identical and that
    // effect does not re-run. That silently broke the requirement that this dialog show every
    // single time the app is opened/resumed for as long as the setting is wrong — it only
    // actually resurfaced after a full process restart (which resets permissionsChecked), not on
    // a simple background→foreground resume. Keying directly on lifecycleRefreshTrigger (which
    // increments on every ON_RESUME, regardless of whether the derived booleans changed) closes
    // that gap.
    LaunchedEffect(lifecycleRefreshTrigger) {
        if (permissionsChecked &&
            !showNotificationPermissionDialog &&
            hasNotificationPermission &&
            shouldPromptBatteryDialog()
        ) {
            showBatteryOptimizationDialog = true
        }
    }
    
    // Always-on collection: LaunchedEffect is tied to the composition lifetime (not Android
    // lifecycle), so Room emissions are NEVER missed — not when on the back stack, not when
    // the app is backgrounded, not during navigation transitions. This prevents the stale-card
    // bug where a deletion on VideoListPage wasn't reflected until the process was killed.
    var recentDownloads by remember { mutableStateOf(emptyList<DownloadedVideoInfo>()) }
    LaunchedEffect(Unit) {
        DatabaseUtil.getVisibleDownloadHistoryFlow().collect { list ->
            recentDownloads = list
        }
    }

    // Tracks IDs that have been hidden this session for instant optimistic UI removal
    var localHiddenIds by remember { mutableStateOf(setOf<Int>()) }

    // Purge stale IDs from localHiddenIds once the DB confirms their removal,
    // so a later re-insertion doesn't get incorrectly suppressed.
    LaunchedEffect(recentDownloads) {
        val currentIds = recentDownloads.map { it.id }.toSet()
        localHiddenIds = localHiddenIds.intersect(currentIds)
    }
    
    // Get recent 5 downloads (remove duplicates by video URL and path to prevent duplicate cards)
    val recentFiveDownloads = remember(recentDownloads) {
        recentDownloads
            .distinctBy { it.videoUrl + it.videoPath } // Use both URL and path to ensure uniqueness
            .takeLast(5)
            .reversed()
    }
    
    // Get active downloads with proper state observation for real-time updates.
    // SnapshotStateMap is a stable reference; derivedStateOf tracks snapshot reads internally.
    val taskStateMap = downloader.getTaskStateMap()

    // Build the set of URLs that currently have an *active* (non-completed) task so we can
    // suppress those entries from the "completed" database section and avoid dual-listing.
    val activeTaskUrls by remember {
        derivedStateOf {
            taskStateMap
                .filter { (_, state) -> state.downloadState !is Task.DownloadState.Completed }
                .keys
                .map { it.url }
                .toSet()
        }
    }

    // Create a comprehensive set of identifiers from recent downloads to avoid duplicates
    val recentDownloadIdentifiers = remember(recentFiveDownloads) {
        recentFiveDownloads.flatMap { download ->
            listOf(
                download.videoUrl,
                download.videoPath,
                "${download.videoUrl}|${download.videoPath}"
            )
        }.toSet()
    }

    // Filter active downloads:
    //   • Always show non-completed tasks (running, queued, paused, canceled, error)
    //   • Show completed tasks only if they haven't yet appeared in the recent-downloads DB section
    // Sort order (strict, stable):
    //   1. Running / FetchingInfo   — actively working right now
    //   2. ReadyWithInfo            — info fetched, waiting for a download slot (more advanced than Idle)
    //   3. Idle                     — just queued, nothing started yet
    //   4. Paused                   — user paused
    //   5. Canceled / Error         — terminal but user-visible
    //   6. Completed                — transition state before DB write, shown below
    // Within each group: newer tasks (higher timeCreated) appear first.
    // IMPORTANT: recentDownloadIdentifiers is a plain Set (not snapshot state), so
    // derivedStateOf cannot track it. We pass it as a key to remember() so the
    // derivedStateOf object is recreated (and re-evaluated) whenever the DB-backed
    // identifiers set changes — e.g. when a completed task is flushed to the DB.
    val activeDownloads by remember(recentDownloadIdentifiers) {
        derivedStateOf {
            taskStateMap
                .filter { (task, state) ->
                    val ds = state.downloadState
                    when {
                        ds is Task.DownloadState.Completed -> {
                            val filePath = ds.filePath
                            val taskUrl  = task.url
                            val isInRecent =
                                recentDownloadIdentifiers.contains(taskUrl) ||
                                recentDownloadIdentifiers.contains(filePath) ||
                                recentDownloadIdentifiers.contains("$taskUrl|$filePath")
                            !isInRecent
                        }
                        else -> true
                    }
                }
                .toList()
                .sortedWith(
                    compareBy<Pair<Task, Task.State>> { (_, state) ->
                        downloadStateSortPriority(state.downloadState)
                    }.thenByDescending { (task, _) -> task.timeCreated }
                )
        }
    }

    // Exclude recent-DB entries whose URL still has a live (non-completed) active task so items
    // don't appear in both sections simultaneously during the Running → Completed transition.
    // Also exclude optimistically hidden items so the card vanishes before the DB Flow re-emits.
    val recentFiveDownloadsFiltered = remember(recentFiveDownloads, activeTaskUrls, localHiddenIds) {
        recentFiveDownloads.filter { it.videoUrl !in activeTaskUrls && it.id !in localHiddenIds }
    }

    // Prune Completed tasks from the in-memory taskStateMap as soon as their DB row is confirmed
    // present. taskStateMap lives inside the process-scoped DownloaderV2 singleton, so a
    // Completed task otherwise lingers there FOREVER (it's only ever removed via the Active
    // Downloads card's own delete button). If left unpruned and the user later deletes/hides
    // that same item from History or Recent Downloads, the DB row disappears but the stale
    // Completed task does not — so it silently satisfies the "not yet synced to DB" condition
    // in activeDownloads below and gets wrongly resurrected as a ghost card. This only became
    // visible after a fresh process start because enqueueFromBackup() never restores Completed
    // tasks, masking the leak on cold boot. Pruning here, right when we know the DB has taken
    // over as source of truth, closes that gap.
    // Checked against the FULL visible download list (not just the last-5 window used for the
    // Recent Downloads cards) so a task that ages out of the top 5 still gets pruned instead of
    // resurfacing indefinitely.
    val allVisibleIdentifiers = remember(recentDownloads) {
        recentDownloads.flatMap { download ->
            listOf(
                download.videoUrl,
                download.videoPath,
                "${download.videoUrl}|${download.videoPath}"
            )
        }.toSet()
    }
    LaunchedEffect(allVisibleIdentifiers) {
        taskStateMap.entries
            .filter { (task, state) ->
                val downloadState = state.downloadState
                downloadState is Task.DownloadState.Completed &&
                    (allVisibleIdentifiers.contains(task.url) ||
                        allVisibleIdentifiers.contains(downloadState.filePath) ||
                        allVisibleIdentifiers.contains("${task.url}|${downloadState.filePath}"))
            }
            .map { it.key }
            .forEach { task -> downloader.remove(task) }
    }

    // Hoisted state for the Spotify-style swipeable download-details sheet: holds the index
    // (within recentFiveDownloadsFiltered) of the card whose details are being viewed, or null
    // when the sheet is closed. Kept as an index (not the item itself) so left/right swipes
    // inside the sheet can move to adjacent list entries.
    var detailsDialogIndex by remember { mutableStateOf<Int?>(null) }
    
    // Handle back press to show exit confirmation
    BackHandler {
        showExitDialog = true
    }
    
    // Notification Permission Dialog
    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNotificationPermissionDialog = false
                if (shouldPromptBatteryDialog()) {
                    showBatteryOptimizationDialog = true
                }
            },
            icon = { 
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { 
                Text(
                    text = stringResource(R.string.notification_permission_required),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { 
                Text(
                    text = stringResource(R.string.notification_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // Open notification settings directly
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            notificationSettingsLauncher.launch(intent)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.grant_permission),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showNotificationPermissionDialog = false
                        if (shouldPromptBatteryDialog()) {
                            showBatteryOptimizationDialog = true
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
    
    // Battery Optimization Dialog — no dismissal state is persisted, so this simply hides for
    // the current screen visit. It will show again on the next app open/resume as long as
    // battery optimization is still not disabled (see shouldPromptBatteryDialog above).
    // The user can opt out of this reminder in General settings (issue #26).
    if (showBatteryOptimizationDialog && !BATTERY_POPUP_HIDDEN.getBoolean()) {
        AlertDialog(
            onDismissRequest = {
                showBatteryOptimizationDialog = false
            },
            icon = { 
                Icon(
                    imageVector = Icons.Outlined.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { 
                Text(
                    text = stringResource(R.string.battery_configuration),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { 
                Column {
                    Text(
                        text = stringResource(R.string.battery_configuration_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val batteryDescResId = remember {
                        BatteryUtil.getBatterySettingsDescResId(BatteryUtil.getManufacturer())
                    }
                    Text(
                        text = stringResource(batteryDescResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryOptimizationDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Wrap in try-catch: on some OEM ROMs (e.g. HarmonyOS 4.x) an
                            // intent may pass resolveActivity() but still throw a
                            // SecurityException or ActivityNotFoundException when launched
                            // due to a restricted permission requirement. Fall back to the
                            // standard Android battery settings in that case.
                            runCatching {
                                val intent = BatteryUtil.buildBatterySettingsIntent(context)
                                batteryOptimizationLauncher.launch(intent)
                            }.onFailure { oemErr ->
                                Log.w("NewHomePage", "OEM battery intent failed, using fallback: ${oemErr.message}")
                                runCatching {
                                    batteryOptimizationLauncher.launch(
                                        BatteryUtil.buildStandardBatteryIntent(context)
                                    )
                                }.onFailure { fallbackErr ->
                                    // ACTION_APPLICATION_DETAILS_SETTINGS should always be
                                    // resolvable; if even this fails, log and do nothing.
                                    Log.e("NewHomePage", "Fallback battery intent also failed: ${fallbackErr.message}")
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.open_settings),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            runCatching {
                                com.tencent.mmkv.MMKV.defaultMMKV().encode(
                                    "battery_prompt_snooze_until",
                                    System.currentTimeMillis() + 30L * 24L * 3600L * 1000L,
                                )
                            }
                            showBatteryOptimizationDialog = false
                        }
                    ) {
                        Text(
                            text = "Don't ask again",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = {
                            showBatteryOptimizationDialog = false
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Exit confirmation dialog

    duplicateUrlPending?.let { dupUrl ->
        AlertDialog(
            onDismissRequest = { duplicateUrlPending = null },
            title = { Text("Already Downloaded") },
            text = {
                Text(
                    "\"${duplicateTitle ?: dupUrl}\" is already in your downloads. Do you want to download it again or queue for Wi-Fi?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        duplicateUrlPending = null
                        urlText = ""
                        dialogViewModel.startDownload(listOf(dupUrl))
                    }
                ) { Text("Download Again") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            WifiQueueManager.enqueueForWifi(dupUrl)
                            duplicateUrlPending = null
                            urlText = ""
                            context.makeToast("Queued for Wi-Fi")
                        }
                    ) { Text("Wi-Fi Queue") }
                    TextButton(onClick = { duplicateUrlPending = null }) { Text("Cancel") }
                }
            },
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = { Icon(Icons.Outlined.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
            title = { Text(stringResource(R.string.exit_app_title)) },
            text = { Text(stringResource(R.string.exit_app_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    }
                ) {
                    Text(stringResource(R.string.exit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.home),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuOpen) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDownloads) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = stringResource(R.string.downloads_history),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Seal+ Branding with animated glowing "+"
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Seal",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        AnimatedGlowingPlus()
                    }
                }
            }

            // Quick-access row for the 4 More Tools — icon-only, no labels/section header per
            // design intent, so it reads as a native strip of shortcuts rather than a separate
            // "section" bolted onto the home screen.
            item {
                QuickToolsRow(
                    onThumbnailDownload = onNavigateToThumbnailDownload,
                    onVideoInfoDownload = onNavigateToVideoInfoDownload,
                    onCommentDownload = onNavigateToCommentDownload,
                    onBatchUrlImport = onNavigateToBatchUrlImport,
                )
            }

            item {
                MediaHubStrip(
                    onOpenYouTubePlayer = onNavigateToStream,
                    onOpenSocialViewer = onNavigateToSocialViewer,
                    onOpenGallery = onNavigateToGallery,
                )
            }
            
            // Smart Clipboard Auto-Detect Banner
            detectedClipboardUrl?.let { clipUrl ->
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 4.dp,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Link detected in clipboard",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = clipUrl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TextButton(
                                onClick = {
                                    view.slightHapticFeedback()
                                    urlText = clipUrl
                                    dismissedClipboardUrl = clipUrl
                                    detectedClipboardUrl = null
                                    if (CONFIGURE.getBoolean() && !clipUrl.contains("list=")) {
                                        fetchSheetUrl = clipUrl
                                    } else {
                                        dialogViewModel.startDownload(listOf(clipUrl))
                                    }
                                }
                            ) {
                                Text("Fetch")
                            }
                            IconButton(
                                onClick = {
                                    dismissedClipboardUrl = clipUrl
                                    detectedClipboardUrl = null
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(Icons.Outlined.Cancel, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // URL Input Field with Download Button
            item {
                URLInputField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    onDownloadClick = {
                        val target = urlText.trim()
                        if (target.isNotBlank()) {
                            view.slightHapticFeedback()
                            keyboardController?.hide()
                            scope.launch(Dispatchers.IO) {
                                val existing = runCatching {
                                    DatabaseUtil.getVideoInfoList().firstOrNull {
                                        it.videoUrl.equals(target, ignoreCase = true)
                                    }
                                }.getOrNull()
                                withContext(Dispatchers.Main) {
                                    if (existing != null) {
                                        duplicateUrlPending = target
                                        duplicateTitle = existing.videoTitle
                                    } else if (CONFIGURE.getBoolean() && !target.contains("list=")) {
                                        fetchSheetUrl = target
                                        urlText = ""
                                    } else {
                                        dialogViewModel.startDownload(listOf(target))
                                        urlText = ""
                                    }
                                }
                            }
                        } else {
                            context.makeToast(R.string.url_empty)
                        }
                    },
                    onPasteClick = {
                        val clipText = clipboardManager.getText()?.text
                        if (clipText != null) {
                            context.matchUrlFromClipboard(clipText)?.let { url ->
                                urlText = url
                                context.makeToast(R.string.paste_msg)
                                scope.launch(Dispatchers.IO) {
                                    DownloadUtil.prefetchVideoInfo(url)
                                }
                            } ?: context.makeToast(R.string.paste_fail_msg)
                        }
                    }
                )
            }
            
            if (wifiQueue.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "${wifiQueue.size} queued for Wi-Fi",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { WifiQueueManager.flushIfOnWifi() }) { Text("Start") }
                            TextButton(onClick = { WifiQueueManager.clearQueue() }) { Text("Clear") }
                        }
                    }
                }
            }

            // Recent Downloads Section - combines both active and completed.
            // Use activeDownloads (not taskStateMap) so the header hides correctly when all
            // tasks are Completed and already present in the DB-backed section.
            if (activeDownloads.isNotEmpty() || recentFiveDownloadsFiltered.isNotEmpty()) {
                item {
                    // Retry All lives here too, so canceled/failed tasks can be restarted from
                    // the home screen without opening the Downloads page.
                    val hasRetryableTasks by remember {
                        derivedStateOf {
                            taskStateMap.values.any {
                                it.downloadState is Task.DownloadState.Canceled ||
                                    it.downloadState is Task.DownloadState.Error
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.recent_downloads),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (hasRetryableTasks) {
                            IconButton(
                                onClick = {
                                    view.slightHapticFeedback()
                                    // Restart every task in a Canceled or Error state.
                                    taskStateMap.entries
                                        .filter { (_, state) ->
                                            state.downloadState is Task.DownloadState.Canceled ||
                                                state.downloadState is Task.DownloadState.Error
                                        }
                                        .map { it.key }
                                        .forEach { downloader.restart(it) }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Replay,
                                    contentDescription = stringResource(R.string.retry_all),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
            
            // Show active downloads first
            if (activeDownloads.isNotEmpty()) {
                items(
                    items = activeDownloads,
                    key = { (task, _) -> task.id }
                ) { (task, state) ->
                    var showDetailsDialog by remember { mutableStateOf(false) }
                    // Combine task+state into a single holder to avoid a TOCTOU window where
                    // two separate state writes could cause a recomposition between them —
                    // detailsTask set but detailsState still null → NPE in DownloadDetailsDialog.
                    var detailsInfo by remember { mutableStateOf<Pair<Task, Task.State>?>(null) }
                    var showActiveDeleteDialog by remember { mutableStateOf(false) }
                    
                    ActiveDownloadCard(
                        task = task,
                        state = state,
                        onAction = { action ->
                            view.slightHapticFeedback()
                            when (action) {
                                UiAction.Pause -> downloader.pause(task)
                                UiAction.Cancel -> downloader.cancel(task)
                                UiAction.Delete -> showActiveDeleteDialog = true
                                UiAction.Resume -> downloader.resume(task)
                                UiAction.Retry -> downloader.restart(task)
                                is UiAction.CopyErrorReport -> {
                                    clipboardManager.setText(
                                        AnnotatedString(getErrorReport(action.throwable, task.url))
                                    )
                                    context.makeToast(R.string.error_copied)
                                }
                                is UiAction.CopyVideoURL -> {
                                    clipboardManager.setText(AnnotatedString(task.url))
                                    context.makeToast(R.string.link_copied)
                                }
                                UiAction.ShowDetails -> {
                                    detailsInfo = task to state
                                    showDetailsDialog = true
                                }
                                is UiAction.OpenFile -> {
                                    action.filePath?.let {
                                        FileUtil.openFile(path = it) { 
                                            context.makeToast(R.string.file_unavailable) 
                                        }
                                    }
                                }
                                is UiAction.OpenThumbnailURL -> {
                                    uriHandler.openUri(action.url)
                                }
                                is UiAction.OpenVideoURL -> {
                                    uriHandler.openUri(action.url)
                                }
                                is UiAction.ShareFile -> {
                                    val shareTitle = context.getString(R.string.share)
                                    FileUtil.createIntentForSharingFile(action.filePath)?.let {
                                        context.startActivity(Intent.createChooser(it, shareTitle))
                                    }
                                }
                            }
                        }
                    )
                    
                    val detailsSnapshot = detailsInfo
                    if (showDetailsDialog && detailsSnapshot != null) {
                        DownloadDetailsDialog(
                            task = detailsSnapshot.first,
                            state = detailsSnapshot.second,
                            onDismiss = { showDetailsDialog = false }
                        )
                    }

                    if (showActiveDeleteDialog) {
                        val deleteTitle = state.viewState.title.ifBlank { task.url }
                        SealDialog(
                            onDismissRequest = { showActiveDeleteDialog = false },
                            title = { Text(text = stringResource(R.string.delete_info)) },
                            icon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            text = {
                                Text(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    text = stringResource(R.string.delete_info_msg).format(deleteTitle),
                                )
                            },
                            confirmButton = {
                                ConfirmButton {
                                    showActiveDeleteDialog = false
                                    scope.launch(Dispatchers.IO) {
                                        val videoId = state.videoInfo?.id
                                        val baseName =
                                            state.videoInfo?.title?.ifBlank { deleteTitle }
                                                ?: deleteTitle
                                        FileUtil.deleteTempFilesForTask(baseName, videoId)
                                        downloader.remove(task)
                                    }
                                }
                            },
                            dismissButton = { DismissButton { showActiveDeleteDialog = false } },
                        )
                    }
                }
            }
            
            // Show recent completed downloads
            if (recentFiveDownloadsFiltered.isNotEmpty()) {
                items(
                    items = recentFiveDownloadsFiltered,
                    key = { it.id }
                ) { downloadInfo ->
                    var showRecentDeleteDialog by remember { mutableStateOf(false) }

                    // rememberUpdatedState ensures the lambdas below always call the LATEST
                    // version of these closures without creating a new lambda instance on every
                    // recomposition. This is critical: during an active download, taskStateMap
                    // updates every ~200ms which triggers recomposition of this item. Without
                    // rememberUpdatedState, each recomposition creates new lambda objects →
                    // RecentDownloadCard sees its parameters as changed → fully recomposes →
                    // the open DropdownMenu loses its composition state and dismisses.
                    // With rememberUpdatedState, the lambda references passed to RecentDownloadCard
                    // are stable across recompositions, so the card only recomposes when
                    // downloadInfo itself actually changes.
                    val currentDownloadInfo by rememberUpdatedState(downloadInfo)
                    val currentList by rememberUpdatedState(recentFiveDownloadsFiltered)
                    val currentLocalHiddenIds by rememberUpdatedState(localHiddenIds)
                    
                    val stableOnClick: () -> Unit = remember {
                        {
                            FileUtil.openFile(currentDownloadInfo.videoPath) {
                                context.makeToast(R.string.file_unavailable)
                            }
                            Unit
                        }
                    }
                    val stableOnShare: () -> Unit = remember {
                        {
                            view.slightHapticFeedback()
                            val shareTitle = context.getString(R.string.share)
                            FileUtil.createIntentForSharingFile(currentDownloadInfo.videoPath)?.let {
                                context.startActivity(Intent.createChooser(it, shareTitle))
                            }
                            Unit
                        }
                    }
                    val stableOnCopyLink: () -> Unit = remember {
                        {
                            view.slightHapticFeedback()
                            clipboardManager.setText(AnnotatedString(currentDownloadInfo.videoUrl))
                            context.makeToast(R.string.link_copied)
                        }
                    }
                    val stableOnShowDetails: () -> Unit = remember {
                        {
                            view.slightHapticFeedback()
                            val idx = currentList.indexOfFirst { it.id == currentDownloadInfo.id }
                            detailsDialogIndex = if (idx >= 0) idx else 0
                        }
                    }
                    val stableOnDelete: () -> Unit = remember {
                        {
                            view.slightHapticFeedback()
                            showRecentDeleteDialog = true
                        }
                    }
                    val stableOnHide: () -> Unit = remember {
                        {
                            view.slightHapticFeedback()
                            localHiddenIds = currentLocalHiddenIds + currentDownloadInfo.id
                            scope.launch(Dispatchers.IO) {
                                DatabaseUtil.hideItem(currentDownloadInfo)
                            }
                            Unit
                        }
                    }
                    
                    RecentDownloadCard(
                        downloadInfo = downloadInfo,
                        refreshKey = lifecycleRefreshTrigger,
                        onClick = stableOnClick,
                        onShare = stableOnShare,
                        onCopyLink = stableOnCopyLink,
                        onShowDetails = stableOnShowDetails,
                        onDelete = stableOnDelete,
                        onHide = stableOnHide,
                    )
                    
                    if (showRecentDeleteDialog) {
                        SealDialog(
                            onDismissRequest = { showRecentDeleteDialog = false },
                            title = { Text(text = stringResource(R.string.delete_info)) },
                            icon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            text = {
                                Text(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    text = stringResource(R.string.delete_info_msg).format(downloadInfo.videoTitle),
                                )
                            },
                            confirmButton = {
                                ConfirmButton {
                                    showRecentDeleteDialog = false
                                    // Use currentDownloadInfo and currentLocalHiddenIds
                                    // (from rememberUpdatedState) so we always act on the
                                    // latest item data — not a stale snapshot captured
                                    // when this lambda was first composed.
                                    val info = currentDownloadInfo
                                    localHiddenIds = currentLocalHiddenIds + info.id
                                    scope.launch(Dispatchers.IO) {
                                        val baseName =
                                            File(info.videoPath)
                                                .nameWithoutExtension
                                                .ifEmpty { info.videoTitle }
                                        FileUtil.deleteTempFilesForTask(baseName, info.videoId)
                                        DatabaseUtil.deleteInfoList(
                                            infoList = listOf(info),
                                            deleteFile = false
                                        )
                                    }
                                }
                            },
                            dismissButton = { DismissButton { showRecentDeleteDialog = false } },
                        )
                    }
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Spotify-style swipeable details sheet for recent downloads. A single sheet instance is
    // shared across all cards; detailsDialogIndex tracks which item (by position in
    // recentFiveDownloadsFiltered) is currently shown, so left/right swipes just move the index.
    detailsDialogIndex?.let { index ->
        // The underlying list is a live Flow and can shrink while the sheet is open (e.g. the
        // user deletes/hides the item they're viewing). Clamp to the last valid index, or close
        // the sheet entirely if nothing is left to show.
        if (recentFiveDownloadsFiltered.isEmpty()) {
            detailsDialogIndex = null
        } else {
            val safeIndex = index.coerceIn(0, recentFiveDownloadsFiltered.lastIndex)
            RecentDownloadDetailsDialog(
                downloadInfoList = recentFiveDownloadsFiltered,
                initialIndex = safeIndex,
                onDismiss = { detailsDialogIndex = null }
            )
        }
    }
    
    // Download Dialog
    var preferences by remember {
        mutableStateOf(DownloadUtil.DownloadPreferences.createFromPreferences())
    }
    val sheetValue by dialogViewModel.sheetValueFlow.collectAsStateWithLifecycle()
    val dialogState by dialogViewModel.sheetStateFlow.collectAsStateWithLifecycle()
    val selectionState = dialogViewModel.selectionStateFlow.collectAsStateWithLifecycle().value
    
    var showDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    LaunchedEffect(sheetValue) {
        if (sheetValue == DownloadDialogViewModel.SheetValue.Expanded) {
            showDialog = true
        } else {
            launch { sheetState.hide(); showDialog = false }
        }
    }
    
    // Config() reads MMKV preferences — memoize it to avoid 5 MMKV reads/sec during
    // active downloads when the 200ms progress throttle triggers recompositions.
    val dialogConfig = remember { Config() }
    if (showDialog) {
        DownloadDialog(
            state = dialogState,
            sheetState = sheetState,
            config = dialogConfig,
            preferences = preferences,
            onPreferencesUpdate = { preferences = it },
            onActionPost = { dialogViewModel.postAction(it) },
        )
    }
    
    when (selectionState) {
        is DownloadDialogViewModel.SelectionState.FormatSelection ->
            FormatPage(
                state = selectionState,
                onDismissRequest = { dialogViewModel.postAction(Action.Reset) },
            )
        
        is DownloadDialogViewModel.SelectionState.PlaylistSelection -> {
            PlaylistSelectionPage(
                state = selectionState,
                onDismissRequest = { dialogViewModel.postAction(Action.Reset) },
            )
        }
        
        DownloadDialogViewModel.SelectionState.Idle -> {}
    }

    // Premium fetch preview (auto info + preset/quality selector, bug #1).
    fetchSheetUrl?.let { pendingUrl ->
        FetchPreviewSheet(
            url = pendingUrl,
            onDismiss = { fetchSheetUrl = null },
            onDownload = { prefs ->
                dialogViewModel.postAction(
                    com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel.Action.DownloadWithPreset(
                        urlList = listOf(pendingUrl),
                        preferences = prefs,
                    )
                )
                fetchSheetUrl = null
            },
        )
    }
}

