package com.junkfood.seal.ui.page.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.UpdateDisabled
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.junkfood.seal.App
import com.junkfood.seal.App.Companion.packageInfo
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.theme.GradientDarkColors
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.LaunchedEffect
import com.junkfood.seal.util.AdBlockFilter
import com.junkfood.seal.util.AUTO_UPDATE
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.junkfood.seal.util.PreferenceUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onNavigateBack: () -> Unit,
    onNavigateToCreditsPage: () -> Unit,
    onNavigateToUpdatePage: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )
    val context = LocalContext.current
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current && isDarkTheme
    var isAutoUpdateEnabled by remember { mutableStateOf(PreferenceUtil.isAutoUpdateEnabled()) }

    val backgroundColor =
        if (isGradientDark) GradientDarkColors.Background else MaterialTheme.colorScheme.background

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.about)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = backgroundColor),
            )
        },
        containerColor = backgroundColor,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Glossy App Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 4.dp,
                    color =
                        if (isGradientDark) GradientDarkColors.GlassSurface.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier =
                        Modifier.fillMaxWidth().then(
                            if (isGradientDark) {
                                Modifier.border(
                                    1.dp,
                                    GradientDarkColors.GlassWhiteBorder,
                                    RoundedCornerShape(24.dp),
                                )
                            } else Modifier
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier.size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.secondaryContainer,
                                            )
                                        )
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.splash_logo),
                                contentDescription = "Seal Next",
                                modifier = Modifier.size(46.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = "v${packageInfo.versionName ?: "1.0"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
            }


            // Data & Storage Stats Card
            item {
                var videoBytes by remember { mutableStateOf(0L) }
                var audioBytes by remember { mutableStateOf(0L) }
                val totalBlocked = remember { AdBlockFilter.getTotalAdsBlocked() }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val vExt = setOf("mp4", "mkv", "webm", "mov", "3gp")
                        val aExt = setOf("m4a", "mp3", "opus", "ogg", "flac", "wav", "aac")
                        var vSum = 0L
                        var aSum = 0L
                        listOf(App.videoDownloadDir, App.audioDownloadDir)
                            .distinct()
                            .map { File(it) }
                            .filter { it.exists() }
                            .flatMap { it.walkTopDown().maxDepth(3).toList() }
                            .filter { it.isFile }
                            .distinctBy { it.absolutePath }
                            .forEach { f ->
                                val ext = f.extension.lowercase()
                                if (ext in vExt) vSum += f.length()
                                else if (ext in aExt) aSum += f.length()
                            }
                        videoBytes = vSum
                        audioBytes = aSum
                    }
                }

                val totalMedia = (videoBytes + audioBytes).coerceAtLeast(1L)
                val videoRatio = (videoBytes.toFloat() / totalMedia.toFloat()).coerceIn(0f, 1f)
                fun fmtBytes(b: Long): String =
                    when {
                        b >= 1_073_741_824 -> "%.2f GB".format(b / 1_073_741_824.0)
                        b >= 1_048_576 -> "%.1f MB".format(b / 1_048_576.0)
                        else -> "%.0f KB".format(b / 1024.0)
                    }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Outlined.Storage, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Data & Storage Stats",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Videos: ${fmtBytes(videoBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "Audio: ${fmtBytes(audioBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (videoBytes > 0L) {
                                Box(
                                    modifier = Modifier
                                        .weight(videoRatio.coerceAtLeast(0.02f))
                                        .height(8.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            if (audioBytes > 0L) {
                                Box(
                                    modifier = Modifier
                                        .weight((1f - videoRatio).coerceAtLeast(0.02f))
                                        .height(8.dp)
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "AdGuard Browser blocked $totalBlocked ads & trackers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.auto_update),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.check_for_updates_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isAutoUpdateEnabled,
                            onCheckedChange = {
                                isAutoUpdateEnabled = !isAutoUpdateEnabled
                                PreferenceUtil.updateValue(AUTO_UPDATE, isAutoUpdateEnabled)
                            },
                            enabled = !App.isFDroidBuild(),
                        )
                    }
                }
            }

            item {
                Card(
                    onClick = onNavigateToUpdatePage,
                    shape = RoundedCornerShape(18.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Check for Updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Manage app & engine versions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Card(
                    onClick = onNavigateToOnboarding,
                    shape = RoundedCornerShape(18.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "View Onboarding",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "See the introduction screens again",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    text = "${stringResource(R.string.app_name)}  •  v${packageInfo.versionName ?: ""}  •  ${context.packageName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun AutoUpdateUnavailableDialog(onDismissRequest: () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton(stringResource(id = R.string.got_it)) { onDismissRequest() }
        },
        icon = {
            Icon(Icons.Outlined.UpdateDisabled, null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text(
                text = stringResource(id = R.string.feature_unavailable),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = "Auto-update is disabled for this build.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
