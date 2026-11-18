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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.theme.GradientDarkColors
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
            DetectedPlatform("X / Twitter", Color(0xFF1DA1F2))
        else -> DetectedPlatform("Media Link", Color(0xFF7C4DFF))
    }
}

@Composable
fun PremiumFetchingContent(
    url: String = "",
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current && isDarkTheme
    val platform = remember(url) { detectPlatformFromUrl(url) }

    val stages = remember {
        listOf(
            "Connecting to server...",
            "Extracting media streams...",
            "Preparing best quality...",
        )
    }
    var stageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1100L)
            stageIndex = (stageIndex + 1) % stages.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fetch_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 260f, 120f),
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Platform pill badge
        Surface(
            shape = CircleShape,
            color = platform.accentColor.copy(alpha = 0.14f),
            modifier = Modifier.border(
                1.dp,
                platform.accentColor.copy(alpha = 0.35f),
                CircleShape,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = platform.accentColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Fast Fetching  •  ${platform.name}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) Color.White else platform.accentColor,
                )
            }
        }

        // Pulsing gradient ring + Seal+ logo
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                platform.accentColor.copy(alpha = 0.22f),
                                Color.Transparent,
                            )
                        )
                    )
            )
            GradientCircularProgressIndicator(
                size = 86.dp,
                strokeWidth = 5.dp,
            )
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
        }

        // Animated stage text
        AnimatedContent(
            targetState = stages[stageIndex],
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            label = "fetch_stage",
        ) { stageText ->
            Text(
                text = stageText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (url.isNotBlank()) {
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        // Shimmering skeleton preview card
        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 2.dp,
            color = if (isGradientDark) {
                GradientDarkColors.GlassSurface.copy(alpha = 0.06f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            modifier = Modifier.fillMaxWidth().then(
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
                    modifier = Modifier
                        .size(width = 96.dp, height = 60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerBrush)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .width(54.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }

        if (onCancel != null) {
            FilledTonalButton(
                onClick = onCancel,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.alpha(0.9f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Cancel")
            }
        }
    }
}
