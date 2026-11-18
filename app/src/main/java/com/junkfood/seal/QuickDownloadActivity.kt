package com.junkfood.seal

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.common.SettingsProvider
import com.junkfood.seal.ui.common.ThemedToastHost
import com.junkfood.seal.ui.component.PremiumFetchingContent
import com.junkfood.seal.ui.component.detectPlatformFromUrl
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel.Action
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel.SelectionState
import com.junkfood.seal.ui.page.downloadv2.configure.FormatPage
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.ui.theme.SealTheme
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.VideoInfo
import com.junkfood.seal.util.matchUrlFromSharedText
import com.junkfood.seal.util.setLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.getViewModel

private sealed interface QuickShareState {
    data object Fetching : QuickShareState

    data class Ready(val info: VideoInfo) : QuickShareState

    data class Started(val title: String) : QuickShareState

    data class Failed(val message: String) : QuickShareState
}

class QuickDownloadActivity : ComponentActivity() {
    private var sharedUrlCached: String = ""

    private fun Intent.getSharedURL(): String? =
        when (action) {
            Intent.ACTION_VIEW -> dataString
            Intent.ACTION_SEND -> {
                getStringExtra(Intent.EXTRA_TEXT)?.let { sharedContent ->
                    removeExtra(Intent.EXTRA_TEXT)
                    matchUrlFromSharedText(sharedContent)
                }
            }
            else -> null
        }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getSharedURL()?.let { sharedUrlCached = it }

        if (sharedUrlCached.isEmpty()) {
            finish()
            return
        }

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT < 33) {
            lifecycleScope.launch(Dispatchers.IO) {
                setLanguage(PreferenceUtil.getLocaleFromPreference())
            }
        }

        val isAudioShortcut = intent.component?.className?.contains("QuickAudioAlias") == true
        val viewModel: DownloadDialogViewModel = getViewModel()

        setContent {
            SettingsProvider(calculateWindowSizeClass(this).widthSizeClass) {
                SealTheme(
                    darkTheme = LocalDarkTheme.current.isDarkTheme(),
                    isHighContrastModeEnabled = LocalDarkTheme.current.isHighContrastModeEnabled,
                ) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    val selectionState =
                        viewModel.selectionStateFlow.collectAsStateWithLifecycle().value
                    val preferences = remember {
                        DownloadUtil.DownloadPreferences.createFromPreferences().let {
                            if (isAudioShortcut) it.copy(extractAudio = true) else it
                        }
                    }
                    var quickState by remember {
                        mutableStateOf<QuickShareState>(QuickShareState.Fetching)
                    }
                    var retryTrigger by remember { mutableIntStateOf(0) }
                    var autoStartPaused by remember { mutableStateOf(false) }

                    // Fast-fetch video info immediately (uses VideoInfoCache if already cached)
                    LaunchedEffect(sharedUrlCached, retryTrigger) {
                        quickState = QuickShareState.Fetching
                        val result =
                            withContext(Dispatchers.IO) {
                                DownloadUtil.fetchVideoInfoFromUrl(
                                    url = sharedUrlCached,
                                    preferences = preferences,
                                )
                            }
                        quickState =
                            result.fold(
                                onSuccess = { QuickShareState.Ready(it) },
                                onFailure = {
                                    QuickShareState.Failed(
                                        it.message ?: "Could not fetch media info"
                                    )
                                },
                            )
                    }

                    fun triggerDownload(audioOnly: Boolean = false, title: String = sharedUrlCached) {
                        val prefs =
                            if (audioOnly) preferences.copy(extractAudio = true) else preferences
                        viewModel.postAction(
                            Action.DownloadWithPreset(
                                urlList = listOf(sharedUrlCached),
                                preferences = prefs,
                            )
                        )
                        quickState = QuickShareState.Started(title)
                    }

                    if (selectionState is SelectionState.FormatSelection) {
                        FormatPage(
                            state = selectionState,
                            onDismissRequest = {
                                viewModel.postAction(Action.Reset)
                                finish()
                            },
                        )
                    } else {
                        ModalBottomSheet(
                            onDismissRequest = { finish() },
                            sheetState = sheetState,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        ) {
                            QuickSharePopupContent(
                                url = sharedUrlCached,
                                state = quickState,
                                autoStartPaused = autoStartPaused,
                                onPauseAutoStart = { autoStartPaused = true },
                                onDownloadNow = { title -> triggerDownload(false, title) },
                                onDownloadAudio = { title -> triggerDownload(true, title) },
                                onChooseQuality = { info ->
                                    autoStartPaused = true
                                    viewModel.postAction(
                                        Action.FetchFormats(
                                            url = sharedUrlCached,
                                            audioOnly = false,
                                            preferences = preferences,
                                        )
                                    )
                                },
                                onDirectDownload = { triggerDownload(false, sharedUrlCached) },
                                onRetry = { retryTrigger++ },
                                onCancel = { finish() },
                                onAutoCompleteFinish = { finish() },
                            )
                        }
                    }

                    ThemedToastHost()
                }
            }
        }
    }
}

@Composable
private fun QuickSharePopupContent(
    url: String,
    state: QuickShareState,
    autoStartPaused: Boolean,
    onPauseAutoStart: () -> Unit,
    onDownloadNow: (String) -> Unit,
    onDownloadAudio: (String) -> Unit,
    onChooseQuality: (VideoInfo) -> Unit,
    onDirectDownload: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onAutoCompleteFinish: () -> Unit,
) {
    val context = LocalContext.current
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current && isDarkTheme
    val platform = remember(url) { detectPlatformFromUrl(url) }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "quick_share_state",
        ) { current ->
            when (current) {
                is QuickShareState.Fetching -> {
                    PremiumFetchingContent(
                        url = url,
                        onCancel = onCancel,
                    )
                }

                is QuickShareState.Ready -> {
                    val info = current.info
                    var countdownProgress by remember { mutableFloatStateOf(0f) }

                    LaunchedEffect(autoStartPaused) {
                        if (!autoStartPaused) {
                            val steps = 30
                            for (i in 1..steps) {
                                delay(50L)
                                countdownProgress = i / steps.toFloat()
                            }
                            onDownloadNow(info.title)
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = platform.accentColor.copy(alpha = 0.14f),
                            ) {
                                Text(
                                    text = platform.name,
                                    color = if (isDarkTheme) Color.White else platform.accentColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                            Text(
                                text = if (autoStartPaused) "Ready to download" else "Auto-starting download...",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Rich Preview Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            tonalElevation = 3.dp,
                            color =
                                if (isGradientDark) GradientDarkColors.GlassSurface.copy(alpha = 0.06f)
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier =
                                Modifier.fillMaxWidth().then(
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
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier =
                                        Modifier.size(width = 104.dp, height = 66.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = info.thumbnail,
                                        contentDescription = info.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().height(66.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = info.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = info.uploader.orEmpty().ifBlank { platform.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        if (!autoStartPaused) {
                            LinearProgressIndicator(
                                progress = { countdownProgress },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = { onDownloadNow(info.title) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(46.dp),
                            ) {
                                Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Download")
                            }
                            FilledTonalButton(
                                onClick = { onDownloadAudio(info.title) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(46.dp),
                            ) {
                                Icon(Icons.Outlined.MusicNote, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Audio")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(
                                onClick = {
                                    onPauseAutoStart()
                                    onChooseQuality(info)
                                }
                            ) {
                                Icon(Icons.Outlined.Tune, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Choose Quality")
                            }
                            TextButton(onClick = onCancel) { Text("Cancel") }
                        }
                    }
                }

                is QuickShareState.Started -> {
                    LaunchedEffect(Unit) {
                        Toast.makeText(context, "Download started!", Toast.LENGTH_SHORT).show()
                        delay(900L)
                        onAutoCompleteFinish()
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp),
                        )
                        Text(
                            text = "Download Started!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                is QuickShareState.Failed -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = "Quick preview unavailable",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onDirectDownload,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Direct Download")
                            }
                            OutlinedButton(
                                onClick = onRetry,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}
