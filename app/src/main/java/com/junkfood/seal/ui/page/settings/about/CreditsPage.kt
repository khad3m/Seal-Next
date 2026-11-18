package com.junkfood.seal.ui.page.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.CreditItem
import com.junkfood.seal.ui.svg.DynamicColorImageVectors
import com.junkfood.seal.ui.svg.drawablevectors.coder

data class Credit(val title: String = "", val license: String? = null, val url: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsPage(onNavigateBack: () -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )

    val creditsList =
        listOf(
            Credit("yt-dlp", "Media extraction engine"),
            Credit("FFmpeg", "Audio & video processing"),
            Credit("NewPipe Extractor", "Fast stream & search resolver"),
            Credit("Media3 ExoPlayer", "Hardware-accelerated playback"),
            Credit("Jetpack Compose & Material 3", "Modern declarative UI"),
            Credit("Coil & OkHttp", "Image & network pipeline"),
            Credit("MMKV & Room", "Fast local persistence"),
        )

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.credits)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
                item {
                    Surface(
                        modifier =
                            Modifier.fillParentMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .clip(MaterialTheme.shapes.large)
                                .clickable {}
                                .clearAndSetSemantics {},
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Image(
                            painter = rememberVectorPainter(image = DynamicColorImageVectors.coder()),
                            contentDescription = null,
                        )
                    }
                }
                items(creditsList) { item ->
                    CreditItem(title = item.title, license = item.license) {}
                }
            }
        },
    )
}
