package com.junkfood.seal.ui.page

import android.webkit.CookieManager
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.theme.GradientDarkColors
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.HapticFeedback.slightHapticFeedback
import com.junkfood.seal.ui.common.LocalWindowWidthState
import com.junkfood.seal.ui.common.Route
import com.junkfood.seal.ui.common.animatedComposable
import com.junkfood.seal.ui.common.animatedComposableVariant
import com.junkfood.seal.ui.common.arg
import com.junkfood.seal.ui.common.id
import com.junkfood.seal.ui.common.slideInVerticallyComposable
import com.junkfood.seal.ui.page.command.TaskListPage
import com.junkfood.seal.ui.page.command.TaskLogPage
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.home.NewHomePage
import com.junkfood.seal.ui.page.onboarding.OnboardingScreen
import com.junkfood.seal.ui.page.settings.SettingsPage
import com.junkfood.seal.ui.page.settings.about.AboutPage
import com.junkfood.seal.ui.page.settings.about.CreditsPage
import com.junkfood.seal.ui.page.settings.about.UpdatePage
import com.junkfood.seal.ui.page.settings.appearance.AppearancePreferences
import com.junkfood.seal.ui.page.settings.appearance.DarkThemePreferences
import com.junkfood.seal.ui.page.settings.appearance.LanguagePage
import com.junkfood.seal.ui.page.settings.command.TemplateEditPage
import com.junkfood.seal.ui.page.settings.command.TemplateListPage
import com.junkfood.seal.ui.page.settings.directory.DownloadDirectoryPreferences
import com.junkfood.seal.ui.page.settings.format.DownloadFormatPreferences
import com.junkfood.seal.ui.page.settings.format.SubtitlePreference
import com.junkfood.seal.ui.page.settings.general.GeneralDownloadPreferences
import com.junkfood.seal.ui.page.settings.interaction.InteractionPreferencePage
import com.junkfood.seal.ui.page.settings.network.CookieProfilePage
import com.junkfood.seal.ui.page.settings.network.CookiesViewModel
import com.junkfood.seal.ui.page.settings.network.NetworkPreferences
import com.junkfood.seal.ui.page.settings.network.WebViewPage
import com.junkfood.seal.ui.page.settings.sealplus.SealPlusExtrasPage
import com.junkfood.seal.ui.page.settings.security.SecuritySettingsPage
import com.junkfood.seal.ui.page.settings.troubleshooting.TroubleShootingPage
import com.junkfood.seal.ui.page.videolist.VideoListPage
import com.junkfood.seal.ui.page.videolist.GalleryPage
import android.net.Uri
import com.junkfood.seal.ui.page.social.SocialViewerPage
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.junkfood.seal.ui.page.stream.MiniPlayerManager
import com.junkfood.seal.ui.page.stream.StreamPage
import com.junkfood.seal.ui.page.hidden.HiddenContentPage
import com.junkfood.seal.ui.page.tools.BatchUrlImportPage
import com.junkfood.seal.ui.page.tools.MoreToolsPage
import com.junkfood.seal.ui.page.tools.CommentDetailPage
import com.junkfood.seal.ui.page.tools.CommentDownloadPage
import com.junkfood.seal.ui.page.tools.ThumbnailDownloadPage
import com.junkfood.seal.ui.page.tools.VideoInfoDetailPage
import com.junkfood.seal.ui.page.tools.VideoInfoDownloadPage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val TAG = "HomeEntry"

private val TopDestinations =
    listOf(Route.HOME, Route.TASK_LIST, Route.SETTINGS_PAGE, Route.DOWNLOADS, Route.MORE_TOOLS)

@Composable
fun AppEntry(dialogViewModel: DownloadDialogViewModel) {

    val navController = rememberNavController()
    val context = LocalContext.current
    val view = LocalView.current
    val windowWidth = LocalWindowWidthState.current
    val sheetState by dialogViewModel.sheetStateFlow.collectAsStateWithLifecycle()
    val cookiesViewModel: CookiesViewModel = koinViewModel()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val versionReport = App.packageInfo.versionName.toString()
    val appName = stringResource(R.string.app_name)
    val scope = rememberCoroutineScope()

    val onNavigateBack: () -> Unit = {
        with(navController) {
            if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                popBackStack()
            }
        }
    }

    if (sheetState is DownloadDialogViewModel.SheetState.Configure) {
        if (navController.currentDestination?.route != Route.HOME) {
            navController.popBackStack(route = Route.HOME, inclusive = false, saveState = true)
        }
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    var currentTopDestination by rememberSaveable { mutableStateOf(currentRoute) }

    LaunchedEffect(currentRoute) {
        if (currentRoute in TopDestinations) {
            currentTopDestination = currentRoute
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavigationDrawer(
            windowWidth = windowWidth,
            drawerState = drawerState,
            currentRoute = currentRoute,
            currentTopDestination = currentTopDestination,
            showQuickSettings = true,
            gesturesEnabled = currentRoute == Route.HOME,
            onDismissRequest = { drawerState.close() },
            onNavigateToRoute = {
                if (currentRoute != it) {
                    navController.navigate(it) {
                        launchSingleTop = true
                        popUpTo(route = Route.HOME)
                    }
                }
            },
        ) {
            NavHost(
                modifier = Modifier.align(Alignment.Center),
                navController = navController,
                startDestination = Route.HOME,
            ) {
                animatedComposable(Route.HOME) {
                    NewHomePage(
                        dialogViewModel = dialogViewModel,
                        onMenuOpen = {
                            view.slightHapticFeedback()
                            scope.launch { drawerState.open() }
                        },
                        onNavigateToDownloads = {
                            view.slightHapticFeedback()
                            navController.navigate(Route.DOWNLOADS) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToBatchUrlImport = {
                            navController.navigate(Route.BATCH_URL_IMPORT) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToVideoInfoDownload = {
                            navController.navigate(Route.VIDEO_INFO_DOWNLOAD) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToThumbnailDownload = {
                            navController.navigate(Route.THUMBNAIL_DOWNLOAD) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToCommentDownload = {
                            navController.navigate(Route.COMMENT_DOWNLOAD) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToStream = {
                            navController.navigate(Route.STREAM) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSocialViewer = {
                            navController.navigate(Route.SOCIAL_VIEWER) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToGallery = {
                            navController.navigate(Route.GALLERY) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                animatedComposable(Route.DOWNLOADS) {
                    VideoListPage(
                        onNavigateBack = { onNavigateBack() },
                        onOpenVault = {
                            navController.navigate(Route.HIDDEN_CONTENT) { launchSingleTop = true }
                        },
                    )
                }
                animatedComposableVariant(Route.TASK_LIST) {
                    TaskListPage(
                        onNavigateBack = onNavigateBack,
                        onNavigateToDetail = { navController.navigate(Route.TASK_LOG id it) },
                    )
                }
                animatedComposable(Route.GALLERY) {
                    GalleryPage(
                        onNavigateBack = onNavigateBack,
                        onOpenVault = {
                            navController.navigate(Route.HIDDEN_CONTENT) { launchSingleTop = true }
                        },
                    )
                }
                animatedComposable(
                    route = "${Route.STREAM}?url={url}",
                    arguments = listOf(
                        navArgument("url") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    ),
                ) { backStackEntry ->
                    val rawUrl = backStackEntry.arguments?.getString("url").orEmpty()
                    StreamPage(
                        onNavigateBack = onNavigateBack,
                        initialUrl = rawUrl.ifBlank { null },
                    )
                }
                animatedComposable(Route.SOCIAL_VIEWER) {
                    SocialViewerPage(
                        onNavigateBack = onNavigateBack,
                        onOpenInPlayer = { url ->
                            navController.navigate("${Route.STREAM}?url=${Uri.encode(url)}") {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                animatedComposable(Route.MORE_TOOLS) {
                    MoreToolsPage(
                        onNavigateBack = onNavigateBack,
                        onNavigateToGallery = { navController.navigate(Route.GALLERY) { launchSingleTop = true } },
                        onNavigateToStream = { navController.navigate(Route.STREAM) { launchSingleTop = true } },
                        onNavigateToSocialViewer = { navController.navigate(Route.SOCIAL_VIEWER) { launchSingleTop = true } },
                        onNavigateToBatchUrlImport = {
                            navController.navigate(Route.BATCH_URL_IMPORT) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToVideoInfoDownload = {
                            navController.navigate(Route.VIDEO_INFO_DOWNLOAD) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToThumbnailDownload = {
                            navController.navigate(Route.THUMBNAIL_DOWNLOAD) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToCommentDownload = {
                            navController.navigate(Route.COMMENT_DOWNLOAD) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                animatedComposable(Route.THUMBNAIL_DOWNLOAD) {
                    ThumbnailDownloadPage(onNavigateBack = onNavigateBack)
                }
                animatedComposable(Route.COMMENT_DOWNLOAD) {
                    CommentDownloadPage(
                        onNavigateBack = onNavigateBack,
                        onNavigateToDetail = {
                            navController.navigate(Route.COMMENT_DETAIL id it) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                slideInVerticallyComposable(
                    Route.COMMENT_DETAIL arg Route.COMMENT_SET_ID,
                    arguments = listOf(navArgument(Route.COMMENT_SET_ID) { type = NavType.IntType }),
                ) {
                    CommentDetailPage(
                        onNavigateBack = onNavigateBack,
                        commentSetId = it.arguments?.getInt(Route.COMMENT_SET_ID) ?: -1,
                    )
                }
                animatedComposable(Route.BATCH_URL_IMPORT) {
                    BatchUrlImportPage(
                        onNavigateBack = onNavigateBack,
                    )
                }
                animatedComposable(Route.VIDEO_INFO_DOWNLOAD) {
                    VideoInfoDownloadPage(
                        onNavigateBack = onNavigateBack,
                        onNavigateToDetail = {
                            navController.navigate(Route.VIDEO_INFO_DETAIL id it) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                slideInVerticallyComposable(
                    Route.VIDEO_INFO_DETAIL arg Route.VIDEO_INFO_ID,
                    arguments = listOf(navArgument(Route.VIDEO_INFO_ID) { type = NavType.IntType }),
                ) {
                    VideoInfoDetailPage(
                        onNavigateBack = onNavigateBack,
                        videoInfoId = it.arguments?.getInt(Route.VIDEO_INFO_ID) ?: -1,
                    )
                }
                slideInVerticallyComposable(
                    Route.TASK_LOG arg Route.TASK_HASHCODE,
                    arguments = listOf(navArgument(Route.TASK_HASHCODE) { type = NavType.IntType }),
                ) {
                    TaskLogPage(
                        onNavigateBack = onNavigateBack,
                        taskHashCode = it.arguments?.getInt(Route.TASK_HASHCODE) ?: -1,
                    )
                }

                settingsGraph(
                    onNavigateBack = onNavigateBack,
                    onNavigateTo = { route ->
                        navController.navigate(route = route) { launchSingleTop = true }
                    },
                    cookiesViewModel = cookiesViewModel,
                )
            }

            val showFloatingDock =
                currentRoute == Route.HOME ||
                    currentRoute?.startsWith(Route.STREAM) == true ||
                    currentRoute == Route.GALLERY ||
                    currentRoute == Route.MORE_TOOLS
            val miniState by MiniPlayerManager.state.collectAsStateWithLifecycle()
            val showMini = miniState.isVisible && currentRoute?.startsWith(Route.STREAM) != true
            val showDockBar =
                showFloatingDock && windowWidth != androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded
            Column(
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .then(if (!showDockBar) Modifier.navigationBarsPadding() else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedVisibility(
                    visible = showMini,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    FloatingMiniPlayerBar(
                        title = miniState.title,
                        uploader = miniState.uploader,
                        thumbnailUrl = miniState.thumbnailUrl,
                        isPlaying = miniState.isPlaying,
                        onOpen = {
                            val targetRoute =
                                if (miniState.sourceUrl.isNotBlank()) {
                                    "${Route.STREAM}?url=${Uri.encode(miniState.sourceUrl)}"
                                } else {
                                    Route.STREAM
                                }
                            navController.navigate(targetRoute) { launchSingleTop = true }
                        },
                        onTogglePlay = { MiniPlayerManager.togglePlayPause() },
                        onClose = { MiniPlayerManager.dismissAndStop() },
                    )
                }
                AnimatedVisibility(
                    visible = showFloatingDock && windowWidth != androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    FloatingGlassNavDock(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        val alreadySelected =
                            currentRoute == route ||
                                (route == Route.STREAM && currentRoute?.startsWith(Route.STREAM) == true)
                        if (!alreadySelected) {
                            navController.navigate(route) {
                                launchSingleTop = true
                                popUpTo(route = Route.HOME)
                            }
                        }
                    },
                    )
                }
            }

            AppUpdater()
            YtdlpUpdater()
        }
    }
}

fun NavGraphBuilder.settingsGraph(
    onNavigateBack: () -> Unit,
    onNavigateTo: (route: String) -> Unit,
    cookiesViewModel: CookiesViewModel,
) {
    navigation(startDestination = Route.SETTINGS_PAGE, route = Route.SETTINGS) {
        animatedComposable(Route.DOWNLOAD_DIRECTORY) {
            DownloadDirectoryPreferences(onNavigateBack)
        }
        animatedComposable(Route.SETTINGS_PAGE) {
            SettingsPage(onNavigateBack = onNavigateBack, onNavigateTo = onNavigateTo)
        }
        animatedComposable(Route.GENERAL_DOWNLOAD_PREFERENCES) {
            GeneralDownloadPreferences(onNavigateBack = { onNavigateBack() }) {
                onNavigateTo(Route.TEMPLATE)
            }
        }
        animatedComposable(Route.DOWNLOAD_FORMAT) {
            DownloadFormatPreferences(onNavigateBack = onNavigateBack) {
                onNavigateTo(Route.SUBTITLE_PREFERENCES)
            }
        }
        animatedComposable(Route.SUBTITLE_PREFERENCES) { SubtitlePreference { onNavigateBack() } }
        animatedComposable(Route.ABOUT) {
            AboutPage(
                onNavigateBack = onNavigateBack,
                onNavigateToCreditsPage = { onNavigateTo(Route.CREDITS) },
                onNavigateToUpdatePage = { onNavigateTo(Route.AUTO_UPDATE) },
                onNavigateToOnboarding = { onNavigateTo(Route.ONBOARDING) },
            )
        }
        animatedComposable(Route.CREDITS) { CreditsPage(onNavigateBack) }
        animatedComposable(Route.AUTO_UPDATE) { UpdatePage(onNavigateBack) }
        animatedComposable(Route.APPEARANCE) {
            AppearancePreferences(onNavigateBack = onNavigateBack, onNavigateTo = onNavigateTo)
        }
        animatedComposable(Route.INTERACTION) { InteractionPreferencePage(onBack = onNavigateBack) }
        animatedComposable(Route.LANGUAGES) { LanguagePage { onNavigateBack() } }
        animatedComposable(Route.DOWNLOAD_DIRECTORY) {
            DownloadDirectoryPreferences { onNavigateBack() }
        }
        animatedComposable(Route.TEMPLATE) {
            TemplateListPage(onNavigateBack = onNavigateBack) {
                onNavigateTo(Route.TEMPLATE_EDIT id it)
            }
        }
        animatedComposable(
            Route.TEMPLATE_EDIT arg Route.TEMPLATE_ID,
            arguments = listOf(navArgument(Route.TEMPLATE_ID) { type = NavType.IntType }),
        ) {
            TemplateEditPage(onNavigateBack, it.arguments?.getInt(Route.TEMPLATE_ID) ?: -1)
        }
        animatedComposable(Route.DARK_THEME) { DarkThemePreferences { onNavigateBack() } }
        animatedComposable(Route.NETWORK_PREFERENCES) {
            NetworkPreferences(
                navigateToCookieProfilePage = { onNavigateTo(Route.COOKIE_PROFILE) }
            ) {
                onNavigateBack()
            }
        }
        animatedComposable(Route.COOKIE_PROFILE) {
            CookieProfilePage(
                cookiesViewModel = cookiesViewModel,
                navigateToCookieGeneratorPage = { onNavigateTo(Route.COOKIE_GENERATOR_WEBVIEW) },
            ) {
                onNavigateBack()
            }
        }
        animatedComposable(Route.COOKIE_GENERATOR_WEBVIEW) {
            WebViewPage(cookiesViewModel = cookiesViewModel) {
                onNavigateBack()
                CookieManager.getInstance().flush()
            }
        }
        animatedComposable(Route.SEALPLUS_EXTRAS) {
            SealPlusExtrasPage(
                onNavigateBack = onNavigateBack,
                onNavigateToSecurity = { onNavigateTo(Route.SECURITY_SETTINGS) },
                onNavigateToHiddenContent = { onNavigateTo(Route.HIDDEN_CONTENT) },
            )
        }
        animatedComposable(Route.HIDDEN_CONTENT) {
            HiddenContentPage(onNavigateBack = onNavigateBack)
        }
        animatedComposable(Route.SECURITY_SETTINGS) {
            SecuritySettingsPage(onBackPressed = onNavigateBack)
        }
        animatedComposable(Route.TROUBLESHOOTING) {
            TroubleShootingPage(onNavigateTo = onNavigateTo, onBack = onNavigateBack)
        }
        animatedComposable(Route.ONBOARDING) {
            OnboardingScreen(onFinish = onNavigateBack)
        }
    }
}


@Composable
private fun FloatingGlassNavDock(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current && isDarkTheme
    val items = listOf(
        Triple(Route.HOME, "Home", Icons.Outlined.Home),
        Triple(Route.STREAM, "YouTube", Icons.Outlined.PlayCircle),
        Triple(Route.SOCIAL_VIEWER, "Browser", Icons.Outlined.Public),
        Triple(Route.GALLERY, "Gallery", Icons.Outlined.VideoLibrary),
    )

    Surface(
        shape = CircleShape,
        tonalElevation = 10.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .then(
                if (isGradientDark) {
                    Modifier.border(1.dp, GradientDarkColors.GlassWhiteBorder, CircleShape)
                } else Modifier
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (route, label, icon) ->
                val selected = currentRoute == route || (route == Route.STREAM && currentRoute?.startsWith(Route.STREAM) == true)
                Surface(
                    onClick = { onNavigate(route) },
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        if (selected) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun FloatingMiniPlayerBar(
    title: String,
    uploader: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    onClose: () -> Unit,
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current && isDarkTheme
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(
                if (isGradientDark) {
                    Modifier.border(1.dp, GradientDarkColors.GlassWhiteBorder, RoundedCornerShape(20.dp))
                } else Modifier
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (uploader.isNotBlank()) {
                    Text(
                        text = uploader,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = "Play/Pause",
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
            }
        }
    }
}
