package com.junkfood.seal.ui.page.hidden

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.ui.common.HapticFeedback.slightHapticFeedback
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.ui.component.SealModalBottomSheetM2
import com.junkfood.seal.ui.page.stream.GlassPlayerSurface
import com.junkfood.seal.ui.svg.DynamicColorImageVectors
import com.junkfood.seal.ui.svg.drawablevectors.videoSteaming
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.LowRamOptimizer
import com.junkfood.seal.util.toFileSizeText
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private data class VaultPlaybackTarget(
    val path: String,
    val title: String,
    val subtitle: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenContentPage(
    viewModel: HiddenContentViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val hiddenList by viewModel.hiddenVideoListFlow.collectAsStateWithLifecycle(emptyList())
    val fileSizeMap by viewModel.fileSizeMapFlow.collectAsStateWithLifecycle(emptyMap())
    val vaultFiles by viewModel.vaultFilesFlow.collectAsStateWithLifecycle(emptyList())

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val hostState = remember { SnackbarHostState() }

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    var showBottomSheet by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var currentInfo by remember { mutableStateOf(DownloadedVideoInfo()) }
    var activePlayback by remember { mutableStateOf<VaultPlaybackTarget?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshVaultFiles()
    }

    BackHandler(sheetState.targetValue == ModalBottomSheetValue.Expanded) {
        scope.launch { sheetState.hide(); showBottomSheet = false }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.hidden_content)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = hostState) },
    ) { innerPadding ->
        if (hiddenList.isEmpty() && vaultFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                val painter = rememberVectorPainter(image = DynamicColorImageVectors.videoSteaming())
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 360.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(vertical = 20.dp)
                            .fillMaxWidth(0.5f)
                            .widthIn(max = 240.dp),
                    )
                    Text(
                        text = stringResource(R.string.no_hidden_content),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (hiddenList.isNotEmpty()) {
                    item(key = "header_db_hidden") {
                        Text(
                            text = "Secure Downloads (${hiddenList.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    items(hiddenList, key = { "db_${it.id}" }) { info ->
                        val sizeText = fileSizeMap.getOrElse(info.id) { 0L }.toFileSizeText()
                        VaultItemCard(
                            title = info.videoTitle,
                            subtitle = listOf(info.videoAuthor, sizeText).filter { it.isNotBlank() }.joinToString("  •  "),
                            isAudio = false,
                            onPlay = {
                                if (File(info.videoPath).exists()) {
                                    activePlayback = VaultPlaybackTarget(
                                        path = info.videoPath,
                                        title = info.videoTitle,
                                        subtitle = info.videoAuthor,
                                    )
                                } else {
                                    Toast.makeText(context, R.string.file_unavailable, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRestore = {
                                viewModel.unhideItem(info)
                                Toast.makeText(context, "Restored to Downloads", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                view.slightHapticFeedback()
                                currentInfo = info
                                scope.launch {
                                    showBottomSheet = true
                                    delay(50)
                                    sheetState.show()
                                }
                            },
                        )
                    }
                }

                if (vaultFiles.isNotEmpty()) {
                    item(key = "header_vault_files") {
                        Text(
                            text = "Private Vault Files (${vaultFiles.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(vaultFiles, key = { "vault_${it.absolutePath}" }) { file ->
                        val isAudio = file.extension.lowercase() in setOf("m4a", "mp3", "opus", "ogg", "flac", "wav", "aac")
                        VaultItemCard(
                            title = file.nameWithoutExtension,
                            subtitle = "${file.extension.uppercase()}  •  ${file.length().toFileSizeText()}",
                            isAudio = isAudio,
                            onPlay = {
                                activePlayback = VaultPlaybackTarget(
                                    path = file.absolutePath,
                                    title = file.nameWithoutExtension,
                                    subtitle = "Private Vault",
                                )
                            },
                            onRestore = {
                                viewModel.restoreVaultFile(file) { ok ->
                                    Toast.makeText(
                                        context,
                                        if (ok) "Restored to Gallery" else "Could not restore file",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                            onDelete = {
                                viewModel.deleteVaultFile(file)
                                Toast.makeText(context, "Deleted from Vault", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
            }
        }
    }

    activePlayback?.let { target ->
        VaultMediaPlayerDialog(
            target = target,
            onDismiss = { activePlayback = null },
        )
    }

    if (showBottomSheet) {
        SealModalBottomSheetM2(
            sheetState = sheetState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                ) {
                    Text(
                        text = currentInfo.videoTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                    showRemoveDialog = true
                                }
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.remove),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val info = currentInfo
                                scope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                    viewModel.unhideItem(info)
                                }
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .weight(2f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.unhide),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            },
        )
    }

    if (showRemoveDialog) {
        SealDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.delete_info)) },
            text = { Text(stringResource(R.string.delete_info_msg).format(currentInfo.videoTitle)) },
            confirmButton = {
                ConfirmButton {
                    scope.launch(Dispatchers.IO) {
                        DatabaseUtil.deleteInfoList(listOf(currentInfo), deleteFile = true)
                    }
                    showRemoveDialog = false
                }
            },
            dismissButton = {
                DismissButton { showRemoveDialog = false }
            },
        )
    }
}

@Composable
private fun VaultItemCard(
    title: String,
    subtitle: String,
    isAudio: Boolean,
    onPlay: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        onClick = onPlay,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isAudio) Icons.Outlined.AudioFile else Icons.Outlined.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FilledTonalIconButton(onClick = onRestore, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = "Restore to original folder",
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun VaultMediaPlayerDialog(
    target: VaultPlaybackTarget,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val player = remember { LowRamOptimizer.createOptimizedExoPlayer(context) }
    var isLandscape by remember { mutableStateOf(true) }

    DisposableEffect(target.path) {
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(target.path))))
        player.prepare()
        player.playWhenReady = true
        val listener =
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(
                        context,
                        error.localizedMessage ?: "Cannot play this vault file",
                        Toast.LENGTH_SHORT,
                    ).show()
                    onDismiss()
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        isLandscape = videoSize.width >= videoSize.height
                    }
                }
            }
        player.addListener(listener)
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            player.removeListener(listener)
            player.release()
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(isLandscape) {
        activity?.requestedOrientation =
            if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            GlassPlayerSurface(
                player = player,
                title = target.title,
                uploader = target.subtitle,
                isFullscreen = true,
                onExitFullscreen = onDismiss,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
