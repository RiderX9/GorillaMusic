package com.gorilla.music.ui.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.LibraryStatus
import com.gorilla.music.ui.components.ScrollableLiquidGlassTabBar
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import kotlinx.coroutines.launch

enum class SettingsTab(val label: String) {
    APPEARANCE("Appearance"), PLAYBACK("Playback"), AUDIO("Audio"), ABOUT("About")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    app: AppViewModel,
    contentPadding: PaddingValues,
    vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val tabs = SettingsTab.entries
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Library-scan feedback: snackbar on completion.
    val status by app.libraryStatus.collectAsStateWithLifecycle()
    val tracks by app.tracks.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var prevStatus by remember { mutableStateOf(status) }
    LaunchedEffect(status) {
        if (prevStatus == LibraryStatus.SCANNING && status == LibraryStatus.READY) {
            snackbarHostState.showSnackbar("Library updated — ${tracks.size} tracks found")
        }
        prevStatus = status
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.bgBase)
    ) {
        Column(Modifier.fillMaxSize()) {
            SettingsHeader()
            ScrollableLiquidGlassTabBar(
                labels = tabs.map { it.label },
                selectedIndex = pagerState.currentPage,
                selectionPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                onSelect = { index ->
                    if (pagerState.currentPage != index) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = index,
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            )
                        }
                    }
                },
                compact = true,
                compactVisibleTabCount = 4,
                compactBottomPadding = 8.dp,
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
            ) { page ->
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = contentPadding.calculateBottomPadding() + 20.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        when (tabs[page]) {
                            SettingsTab.APPEARANCE -> AppearanceTab(settings, vm)
                            SettingsTab.PLAYBACK -> PlaybackTab(settings, vm)
                            SettingsTab.AUDIO -> AudioTab(settings, vm)
                            SettingsTab.ABOUT -> AboutTab(app)
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun SettingsHeader() {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "PREFERENCES",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = "Settings",
                color = appColors.textPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 38.sp,
            )
        }
    }
}
