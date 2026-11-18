package com.junkfood.seal.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class DetectedPlatform(val name: String, val accentColor: Color)

fun detectPlatformFromUrl(url: String): DetectedPlatform {
    val lower = url.lowercase()
    return when {
        lower.contains("youtube.com") || lower.contains("youtu.be") ->
            DetectedPlatform("YouTube", Color(0xFFFF0033))
        lower.contains("instagram.com") || lower.contains("instagr.am") ->
            DetectedPlatform("Instagram", Color(0xFFE1306C))
        lower.contains("tiktok.com") ->
            DetectedPlatform("TikTok", Color(0xFF00C2CB))
        lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com") ->
            DetectedPlatform("Facebook", Color(0xFF1877F2))
        lower.contains("twitter.com") || lower.contains("x.com") ->
            DetectedPlatform("X", Color(0xFF1DA1F2))
        else -> DetectedPlatform("Link", Color(0xFF6750A4))
    }
}

@Composable
fun PremiumFetchingContent(
    url: String = "",
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val platform = remember(url) { detectPlatformFromUrl(url) }
    val stages = remember {
        listOf(
            "Connecting...",
            "Loading media info...",
            "Checking available formats...",
        )
    }
    var stageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1400L)
            if (stageIndex < stages.lastIndex) stageIndex++
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fetch_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
        ),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 240f, 120f),
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(
                color = platform.accentColor,
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.size(22.dp),
            )
            AnimatedContent(
                targetState = stages[stageIndex],
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "fetch_stage",
            ) { stageText ->
                Text(
                    text = stageText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (url.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(platform.accentColor)
                    )
                    Text(
                        text = url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Clean Material 3 skeleton preview card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 104.dp, height = 62.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(shimmerBrush)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }

        if (onCancel != null) {
            TextButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Cancel")
            }
        }
    }
}
