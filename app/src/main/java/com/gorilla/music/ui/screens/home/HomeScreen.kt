package com.gorilla.music.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.music.data.db.PlaylistSummary
import com.gorilla.music.data.model.RadioStation
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.repo.PlayedTrack
import com.gorilla.music.data.repo.toTrack
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.components.AlbumArt
import com.gorilla.music.ui.components.EmptyState
import com.gorilla.music.ui.components.formatDuration
import com.gorilla.music.ui.screens.library.LibraryTab
import com.gorilla.music.ui.screens.radio.RadioUiState
import com.gorilla.music.ui.screens.radio.RadioViewModel
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.accentBloom
import com.gorilla.music.ui.theme.instantCombinedClickable
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.instantCombinedClickable
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    app: AppViewModel,
    contentPadding: PaddingValues,
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    radioVm: RadioViewModel = viewModel(factory = RadioViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val radioState by radioVm.uiState.collectAsStateWithLifecycle()
    val playback by app.playbackState.collectAsStateWithLifecycle()
    val lastRadioStationId by app.lastRadioStationId.collectAsStateWithLifecycle()
    val listeningTimeMs by app.actualListeningTimeMs.collectAsStateWithLifecycle()
    val appColors = LocalAppColors.current
    var showMostPlayed by remember { mutableStateOf(false) }
    var recentExpanded by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(appColors.bgBase),
    ) {
        when (val ui = state) {
            HomeUiState.Loading -> Spacer(Modifier.fillMaxSize())
            HomeUiState.Empty -> EmptyState(
                title = "Your library is empty",
                subtitle = "Add music to your device, then scan from Settings > About.",
            )
            is HomeUiState.Success -> {
                val radioStations = (radioState as? RadioUiState.Success)?.stations.orEmpty()
                val highlightedStation = radioStations.firstOrNull {
                    playback.current?.folder == "radio" && it.id == playback.current?.id
                }
                    ?: radioStations.firstOrNull { it.id == lastRadioStationId }
                    ?: radioStations.firstOrNull()
                val stableRecentIds = remember { mutableListOf<Long>() }
                val recentTracks = remember(ui.tracks, ui.recent) {
                    val incoming = (ui.recent.ifEmpty { ui.tracks }).take(12)
                    if (stableRecentIds.isEmpty() || incoming.size != stableRecentIds.size) {
                        stableRecentIds.clear()
                        stableRecentIds.addAll(incoming.map { it.id })
                        incoming
                    } else {
                        val firstIncomingId = incoming.firstOrNull()?.id
                        if (firstIncomingId != null && firstIncomingId !in stableRecentIds) {
                            stableRecentIds.clear()
                            stableRecentIds.addAll(incoming.map { it.id })
                            incoming
                        } else {
                            stableRecentIds.mapNotNull { id ->
                                incoming.find { it.id == id } ?: ui.tracks.find { it.id == id }
                            }
                        }
                    }
                }
                val topArtists = remember(ui.tracks) {
                    buildTopArtists(ui.tracks, ui.mostPlayed)
                }
                val favorites = remember(ui.tracks) { ui.tracks.filter { it.isFavorite } }

                AnimatedContent(
                    targetState = showMostPlayed,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn())
                                .togetherWith(slideOutHorizontally(tween(260)) { -it / 5 } + fadeOut())
                        } else {
                            (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn())
                                .togetherWith(slideOutHorizontally(tween(260)) { it / 5 } + fadeOut())
                        }
                    },
                    label = "mostPlayedTransition",
                    modifier = Modifier.fillMaxSize(),
                ) { showingMostPlayed ->
                    if (showingMostPlayed) {
                        MostPlayedScreen(
                            app = app,
                            ui = ui,
                            playback = playback,
                            contentPadding = contentPadding,
                            onBack = { showMostPlayed = false },
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            ListenNowHeader()

                            val listState = rememberLazyListState()
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                item {
                                    HomeSectionHeader(
                                        eyebrow = "TOP ROTATION",
                                        title = "Your Favourite Artists",
                                        action = "See All",
                                        onAction = {
                                            app.requestLibraryTab(LibraryTab.ARTISTS)
                                            onNavigateToLibrary()
                                        },
                                    )
                                    FavoriteArtistsRow(
                                        artists = topArtists,
                                        onPlayArtist = { artist ->
                                            app.playTrack(artist.tracks.first(), artist.tracks)
                                        },
                                    )
                                }
                                item {
                                    HomeSectionHeader(
                                        eyebrow = "QUICK MODULES",
                                        title = "Activity & Radar",
                                    )
                                    ActivityGrid(
                                        trackCount = ui.tracks.size,
                                        favoriteCount = favorites.size,
                                        listeningTimeMs = listeningTimeMs,
                                        station = highlightedStation,
                                        isRadioPlaying = playback.current?.id == highlightedStation?.id && playback.isPlaying,
                                        onShuffle = app::shuffleAll,
                                        onOpenStats = { showMostPlayed = true },
                                        onPlayStation = { station ->
                                            if (playback.current?.id == station.id) {
                                                app.togglePlayPause()
                                            } else {
                                                app.playTrack(station.toTrack(), radioStations.map { it.toTrack() })
                                            }
                                        },
                                        onOpenFavorites = {
                                            app.requestLibraryTab(LibraryTab.FAVORITES)
                                            onNavigateToLibrary()
                                        },
                                    )
                                }
                                item {
                                    HomeSectionHeader(
                                        eyebrow = "JUMP BACK IN",
                                        title = "Recent Tracks",
                                        action = when {
                                            recentTracks.size <= 6 -> null
                                            recentExpanded -> "Less"
                                            else -> "More"
                                        },
                                        onAction = { recentExpanded = !recentExpanded },
                                    )
                                    ExpandableRecentTracks(
                                        tracks = recentTracks,
                                        expanded = recentExpanded,
                                        activeTrackId = playback.current?.id,
                                        isPlaying = playback.isPlaying,
                                        onPlay = { track ->
                                            if (playback.current?.id == track.id) {
                                                app.togglePlayPause()
                                            } else {
                                                app.playTrack(track, recentTracks)
                                            }
                                        },
                                        onLongClick = { track ->
                                            app.openTrackMenu(track, recentTracks)
                                        },
                                    )
                                }
                                item {
                                    HomeSectionHeader(
                                        eyebrow = "COMING UP",
                                        title = "Up Next",
                                    )
                                    UpNextQueuePill(
                                        playbackQueue = playback.queue,
                                        currentIndex = playback.currentIndex,
                                        onPlayNext = { index ->
                                            app.playback.skipToQueueItem(index)
                                        },
                                    )
                                }
                                item {
                                    HomeSectionHeader(
                                        eyebrow = "PINNED & RECENT",
                                        title = "Your Playlists",
                                        action = "See All",
                                        onAction = onNavigateToPlaylists,
                                    )
                                    HomePlaylistsRow(
                                        playlists = ui.playlists.take(8),
                                        tracks = ui.tracks,
                                        onOpen = { playlistId ->
                                            app.requestPlaylist(playlistId)
                                            onNavigateToPlaylists()
                                        },
                                        onPin = { id, pinned -> app.setPlaylistPinned(id, pinned) },
                                        onMove = { id, direction -> app.movePlaylist(id, direction) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListenNowHeader() {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp),
    ) {
        Text(
            text = "GORILLAMUSIC",
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
        )
        Text(
            text = "Listen Now",
            color = appColors.textPrimary,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

private data class HomeArtist(
    val name: String,
    val representative: Track,
    val tracks: List<Track>,
    val score: Int,
)

private fun buildTopArtists(
    tracks: List<Track>,
    mostPlayed: List<PlayedTrack>,
): List<HomeArtist> {
    val playsByArtist = mostPlayed
        .groupBy { it.track.displayArtist }
        .mapValues { (_, played) -> played.sumOf { it.playCount } }

    return tracks
        .groupBy { it.displayArtist }
        .map { (artist, artistTracks) ->
            val representative = artistTracks.firstOrNull { it.artworkUri != null }
                ?: artistTracks.first()
            HomeArtist(
                name = artist,
                representative = representative,
                tracks = artistTracks,
                score = playsByArtist[artist] ?: artistTracks.size,
            )
        }
        .sortedWith(compareByDescending<HomeArtist> { it.score }.thenBy { it.name.lowercase() })
        .take(8)
}

@Composable
private fun HomeSectionHeader(
    eyebrow: String,
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = eyebrow,
                color = appColors.textSecondary.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(
                text = title,
                color = appColors.textPrimary,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (action != null) {
            Text(
                text = action,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 12.dp, bottom = 2.dp)
                    .then(
                        if (onAction != null) {
                            Modifier.instantClickable(pressedScale = 0.92f, onClick = onAction)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun FavoriteArtistsRow(
    artists: List<HomeArtist>,
    onPlayArtist: (HomeArtist) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val horizontalPadding = 20.dp
        val itemSpacing = 20.dp
        val artistSlotSize = (
            maxWidth - horizontalPadding * 2 - itemSpacing * 3
        ) / 4
        val artistArtworkSize = artistSlotSize

        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            items(artists, key = { it.name }) { artist ->
                Column(
                    modifier = Modifier
                        .width(artistSlotSize)
                        .instantClickable(pressedScale = 0.94f) { onPlayArtist(artist) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AlbumArt(
                        albumId = artist.representative.albumId,
                        artworkUri = artist.representative.artworkUri,
                        modifier = Modifier
                            .size(artistArtworkSize)
                            .border(1.5.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                        shape = CircleShape,
                    )
                    Text(
                        text = artist.name,
                        color = LocalAppColors.current.textPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityGrid(
    trackCount: Int,
    favoriteCount: Int,
    listeningTimeMs: Long,
    station: RadioStation?,
    isRadioPlaying: Boolean,
    onShuffle: () -> Unit,
    onOpenStats: () -> Unit,
    onPlayStation: (RadioStation) -> Unit,
    onOpenFavorites: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActivityCompactCard(
                label = "TELEMETRY",
                value = formatListeningTime(listeningTimeMs),
                subtitle = "Listening Stats",
                buttonText = "Stats",
                icon = Icons.Rounded.AccessTime,
                colors = listOf(Color(0xFF062340), Color(0xFF041221)),
                accent = Color(0xFF00BFFF),
                decoration = ActivityCardDecoration.Wave,
                onClick = onOpenStats,
                modifier = Modifier.weight(1f),
            )
            ActivityCompactCard(
                label = "LIVE ON AIR",
                value = station?.name ?: "Radio",
                subtitle = station?.country?.ifBlank { "Online Station" } ?: "Finding Stations",
                buttonText = if (isRadioPlaying) "Pause" else "Listen",
                icon = Icons.Rounded.Radio,
                colors = listOf(Color(0xFF4D0A13), Color(0xFF2A0308)),
                accent = Color(0xFFFB5A6E),
                decoration = ActivityCardDecoration.Rings,
                onClick = { station?.let(onPlayStation) },
                enabled = station != null,
                marqueeText = isRadioPlaying,
                modifier = Modifier.weight(1f),
            )
        }
        ActivityControlBar(
            trackCount = trackCount,
            favoriteCount = favoriteCount,
            onShuffle = onShuffle,
            onOpenFavorites = onOpenFavorites,
        )
    }
}

private enum class ActivityCardDecoration {
    Wave,
    Rings,
}

@Composable
private fun ActivityCompactCard(
    label: String,
    value: String,
    subtitle: String,
    buttonText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: List<Color>,
    accent: Color,
    decoration: ActivityCardDecoration,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    marqueeText: Boolean = false,
) {
    var marqueeRun by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var lastRunTime by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val shape = RoundedCornerShape(16.dp)

    LaunchedEffect(marqueeText) {
        if (marqueeText) {
            val timeSinceLastRun = System.currentTimeMillis() - lastRunTime
            if (timeSinceLastRun < 17_000 && lastRunTime > 0) {
                kotlinx.coroutines.delay(17_000 - timeSinceLastRun)
            }
            while (true) {
                marqueeRun++
                lastRunTime = System.currentTimeMillis()
                kotlinx.coroutines.delay(17_000)
            }
        }
    }
    Box(
        modifier = modifier
            .height(90.dp)
            .then(
                if (enabled) {
                    Modifier.instantClickable(pressedScale = 0.96f, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .border(1.dp, accent.copy(alpha = 0.25f), shape),
    ) {
        when (decoration) {
            ActivityCardDecoration.Wave -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp)
                        .align(Alignment.BottomCenter),
                ) {
                    val scaleX = size.width / 150f
                    val scaleY = size.height / 40f
                    val wave = Path().apply {
                        moveTo(0f, 20f * scaleY)
                        quadraticBezierTo(
                            35f * scaleX,
                            5f * scaleY,
                            75f * scaleX,
                            15f * scaleY,
                        )
                        quadraticBezierTo(
                            115f * scaleX,
                            25f * scaleY,
                            150f * scaleX,
                            10f * scaleY,
                        )
                    }
                    val waveFill = Path().apply {
                        addPath(wave)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    val waveColor = Color(0xFF38BDF8)
                    drawPath(
                        path = waveFill,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                waveColor.copy(alpha = 0.28f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    drawPath(
                        path = wave,
                        color = waveColor,
                        style = Stroke(width = 2.5.dp.toPx()),
                    )
                }
            }
            ActivityCardDecoration.Rings -> {
                Canvas(Modifier.fillMaxSize()) {
                    val center = androidx.compose.ui.geometry.Offset(
                        size.width - 22.dp.toPx(),
                        19.dp.toPx(),
                    )
                    drawCircle(
                        color = accent.copy(alpha = 0.10f),
                        radius = 35.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                    drawCircle(
                        color = accent.copy(alpha = 0.15f),
                        radius = 23.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = label,
                    color = accent.copy(alpha = if (enabled) 1f else 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent.copy(alpha = if (enabled) 1f else 0.5f),
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(16.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    key(marqueeRun) {
                        Text(
                            text = value,
                            color = Color.White.copy(alpha = if (enabled) 1f else 0.55f),
                            fontSize = 15.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.basicMarquee(
                                iterations = if (marqueeRun > 0) 1 else 0,
                                initialDelayMillis = 250,
                                velocity = 24.dp,
                            ),
                        )
                    }
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = if (enabled) 0.55f else 0.35f),
                        fontSize = 10.5.sp,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = if (enabled) 1f else 0.35f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = buttonText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityControlBar(
    trackCount: Int,
    favoriteCount: Int,
    onShuffle: () -> Unit,
    onOpenFavorites: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(appColors.bgSurface.copy(alpha = 0.94f))
            .border(1.dp, appColors.borderGlass.copy(alpha = 0.78f), shape)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityBarAction(
            title = "Quick Shuffle",
            subtitle = "$trackCount local tracks",
            icon = Icons.Rounded.Shuffle,
            accent = Color(0xFF34D399),
            trailingIcon = Icons.Rounded.PlayArrow,
            trailingBackground = Color(0xFF10B981),
            trailingTint = Color.White,
            onClick = onShuffle,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(36.dp)
                .background(appColors.borderGlass.copy(alpha = 0.78f)),
        )
        ActivityBarAction(
            title = "Favorites",
            subtitle = "$favoriteCount starred songs",
            icon = Icons.Rounded.Favorite,
            accent = Color(0xFFC084FC),
            trailingIcon = Icons.Rounded.ChevronRight,
            trailingBackground = Color(0xFFB06EE8),
            trailingTint = Color.White,
            onClick = onOpenFavorites,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActivityBarAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingBackground: Color,
    trailingTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .instantClickable(pressedScale = 0.96f, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.5.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(trailingBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = trailingTint,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpandableRecentTracks(
    tracks: List<Track>,
    expanded: Boolean,
    activeTrackId: Long?,
    isPlaying: Boolean,
    onPlay: (Track) -> Unit,
    onLongClick: (Track) -> Unit,
) {
    Column {
        RecentTracksRow(
            tracks = tracks.take(6),
            activeTrackId = activeTrackId,
            isPlaying = isPlaying,
            onPlay = onPlay,
            onLongClick = onLongClick,
        )
        AnimatedVisibility(
            visible = expanded && tracks.size > 6,
            enter = expandVertically(
                animationSpec = tween(340, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top,
            ) + fadeIn(animationSpec = tween(220, delayMillis = 70)),
            exit = shrinkVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(animationSpec = tween(160)),
        ) {
            RecentTracksRow(
                tracks = tracks.drop(6).take(6),
                activeTrackId = activeTrackId,
                isPlaying = isPlaying,
                onPlay = onPlay,
                onLongClick = onLongClick,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentTracksRow(
    tracks: List<Track>,
    activeTrackId: Long?,
    isPlaying: Boolean,
    onPlay: (Track) -> Unit,
    onLongClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val buttonContent = if (accent.luminance() > 0.55f) Color.Black.copy(alpha = 0.78f) else Color.White
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(tracks, key = { it.id }) { track ->
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .instantCombinedClickable(
                        pressedScale = 0.96f,
                        onClick = { onPlay(track) },
                        onLongClick = { onLongClick(track) },
                    ),
            ) {
                Box {
                    AlbumArt(
                        albumId = track.albumId,
                        artworkUri = track.artworkUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (activeTrackId == track.id && isPlaying) {
                                Icons.Rounded.Pause
                            } else {
                                Icons.Rounded.PlayArrow
                            },
                            contentDescription = if (activeTrackId == track.id && isPlaying) "Pause" else "Play",
                            tint = buttonContent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = track.title,
                    color = if (activeTrackId == track.id) accent else appColors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = track.displayArtist,
                    color = appColors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun UpNextQueuePill(
    playbackQueue: List<Track>,
    currentIndex: Int,
    onPlayNext: (Int) -> Unit,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val upcomingStart = if (currentIndex in playbackQueue.indices) currentIndex + 1 else 0
    val upcoming = playbackQueue.drop(upcomingStart)
    val next = upcoming.firstOrNull()
    val nextIndex = if (next == null) -1 else upcomingStart
    val remaining = (upcoming.size - 1).coerceAtLeast(0)
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(94.dp)
            .then(
                if (nextIndex >= 0) {
                    Modifier.instantClickable(pressedScale = 0.97f) { onPlayNext(nextIndex) }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
            .clip(shape)
            .background(appColors.bgSurface.copy(alpha = 0.94f))
            .border(1.dp, appColors.borderGlass.copy(alpha = 0.78f), shape)
            .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QueueArtStack(
                tracks = upcoming.take(3),
                modifier = Modifier.size(width = 60.dp, height = 52.dp),
            )
            Column(Modifier.weight(1f)) {
            Text(
                text = "NEXT TRACK",
                color = appColors.textSecondary.copy(alpha = 0.72f),
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
            Text(
                text = next?.title ?: "Nothing queued",
                color = appColors.textPrimary,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = next?.displayArtist ?: "Add tracks from your library",
                color = appColors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    next == null -> "Queue is empty"
                    remaining == 0 -> "Last track in queue"
                    else -> "+$remaining more in queue"
                },
                color = accent,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 3.dp),
            )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        if (nextIndex >= 0) {
                            Modifier.instantClickable(pressedScale = 0.90f) { onPlayNext(nextIndex) }
                        } else {
                            Modifier
                        }
                    )
                    .clip(CircleShape)
                    .background(appColors.bgGlass)
                    .border(1.dp, appColors.borderGlass.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (next == null) Icons.AutoMirrored.Rounded.QueueMusic else Icons.Rounded.SkipNext,
                    contentDescription = if (next == null) "Queue is empty" else "Play next track",
                    tint = appColors.textPrimary.copy(alpha = if (next == null) 0.6f else 0.92f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun QueueArtStack(
    tracks: List<Track>,
    modifier: Modifier = Modifier,
) {
    val visibleTracks = tracks.take(3)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        visibleTracks.asReversed().forEachIndexed { drawIndex, track ->
            val queueIndex = visibleTracks.lastIndex - drawIndex
            val isFront = queueIndex == 0
            AlbumArt(
                albumId = track.albumId,
                artworkUri = track.artworkUri,
                modifier = Modifier
                    .size(46.dp)
                    .offset(x = (queueIndex * 6).dp, y = (queueIndex * 3).dp)
                    .rotate(
                        when (queueIndex) {
                            1 -> -3f
                            2 -> 6f
                            else -> 0f
                        },
                    )
                    .zIndex((visibleTracks.size - queueIndex).toFloat())
                    .border(
                        1.dp,
                        Color.White.copy(alpha = if (isFront) 0.22f else 0.12f),
                        RoundedCornerShape(13.dp),
                    )
                    .clip(RoundedCornerShape(13.dp)),
                shape = RoundedCornerShape(13.dp),
            )
        }
        if (visibleTracks.isEmpty()) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(LocalDynamicColors.current.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = LocalDynamicColors.current.accent,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun HomePlaylistsRow(
    playlists: List<PlaylistSummary>,
    tracks: List<Track>,
    onOpen: (Long) -> Unit,
    onPin: (Long, Boolean) -> Unit,
    onMove: (Long, Int) -> Unit,
) {
    val appColors = LocalAppColors.current
    var contextMenuFor by remember { mutableStateOf<PlaylistSummary?>(null) }
    
    if (contextMenuFor != null) {
        Dialog(onDismissRequest = { contextMenuFor = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(appColors.bgSurface)
                    .border(1.dp, appColors.borderGlass, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = contextMenuFor!!.name,
                    color = appColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val actionModifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(appColors.bgGlass)
                    .padding(16.dp)

                val accent = LocalDynamicColors.current.accent

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (contextMenuFor!!.isPinned) Color(0xFFEF4444).copy(alpha = 0.15f) else appColors.bgGlass).clickable { 
                        onPin(contextMenuFor!!.id, !contextMenuFor!!.isPinned)
                        contextMenuFor = null 
                    }.padding(16.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pinColor = if (contextMenuFor!!.isPinned) Color(0xFFEF4444) else accent
                    Icon(Icons.Rounded.PushPin, contentDescription = "Pin", tint = pinColor, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(if (contextMenuFor!!.isPinned) "Unpin from start" else "Pin to start", color = pinColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(appColors.bgGlass).clickable { 
                        onMove(contextMenuFor!!.id, -1)
                        contextMenuFor = null 
                    }.padding(16.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Move Left", tint = accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Move Left", color = appColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(appColors.bgGlass).clickable { 
                        onMove(contextMenuFor!!.id, 1)
                        contextMenuFor = null 
                    }.padding(16.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Move Right", tint = accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Move Right", color = appColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (playlists.isEmpty()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(appColors.bgSurface.copy(alpha = 0.8f))
                .border(1.dp, appColors.borderGlass.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .instantClickable(pressedScale = 0.98f, onClick = { onOpen(-1L) }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                tint = LocalDynamicColors.current.accent,
                modifier = Modifier.size(24.dp),
            )
            Column(Modifier.weight(1f)) {
                Text("No playlists yet", color = appColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Create one in your library", color = appColors.textSecondary, fontSize = 12.sp)
            }
        }
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(playlists, key = { it.id }) { playlist ->
            val representative = tracks.firstOrNull { it.id == playlist.firstTrackId }
            val colors = playlistColors(playlist.id)
            Box(
                modifier = Modifier
                    .width(if (playlist == playlists.first()) 220.dp else 172.dp)
                    .height(172.dp)
                    .instantCombinedClickable(
                        pressedScale = 0.97f,
                        onClick = { onOpen(playlist.id) },
                        onLongClick = { contextMenuFor = playlist }
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                ) {
                if (representative != null) {
                    AlbumArt(
                        albumId = representative.albumId,
                        artworkUri = representative.artworkUri,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(22.dp),
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(colors)))
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f)),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                ) {
                    Text(
                        text = playlist.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${playlist.trackCount} ${if (playlist.trackCount == 1) "song" else "songs"}${if (playlist.isPinned) " · Pinned" else ""}",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
    }
}

private fun playlistColors(id: Long): List<Color> {
    return when ((id % 4).toInt()) {
        0 -> listOf(Color(0xFF8A3A52), Color(0xFF4A1A2C))
        1 -> listOf(Color(0xFFA97A3A), Color(0xFF4A3216))
        2 -> listOf(Color(0xFF1F3F66), Color(0xFF0D1D33))
        else -> listOf(Color(0xFF175A47), Color(0xFF0A2A20))
    }
}

@Composable
private fun HomeHeroSpotlight(
    track: Track?,
    onClick: () -> Unit,
) {
    val haptic = rememberHaptic()
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF022C22),
                        Color(0xFF064E3B),
                        Color(0xFF047857),
                    )
                )
            )
            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(26.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (track != null) {
                AlbumArt(
                    albumId = track.albumId,
                    artworkUri = track.artworkUri,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF1F3F66), Color(0xFF0D1D33))))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🦍", fontSize = 20.sp)
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = "🦍 Smart Mix",
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    text = "Daily Music Radar",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Auto-generated local shuffle",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .instantClickable(pressedScale = 0.94f) {
                    haptic()
                    onClick()
                }
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = appColors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.2).sp,
        )
        if (action != null) {
            val haptic = rememberHaptic()
            Text(
                text = action,
                color = accent,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .then(
                        if (onAction != null) {
                            Modifier
                                .instantClickable(pressedScale = 0.92f) {
                                    haptic()
                                    onAction()
                                }
                        } else {
                            Modifier
                        }
                    ),
            )
        }
    }
}

private enum class TrackAction { PLAY, FAVORITE }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HomeTrackRow(
    track: Track,
    subtitle: String,
    isActive: Boolean,
    isPlaying: Boolean = false,
    onPlay: () -> Unit,
    onLongClick: () -> Unit,
    action: TrackAction = TrackAction.PLAY,
) {
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .instantClickable(pressedScale = 0.96f) {
                onPlay()
            }
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isActive) {
                    Brush.linearGradient(
                        listOf(
                            LocalDynamicColors.current.accent.copy(alpha = 0.22f),
                            appColors.bgGlass,
                        ),
                    )
                } else {
                    Brush.linearGradient(listOf(appColors.bgGlass, appColors.bgGlass))
                },
            )
            .border(
                1.dp,
                if (isActive) LocalDynamicColors.current.accent.copy(alpha = 0.4f) else appColors.borderGlass.copy(alpha = 0.65f),
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AlbumArt(
            albumId = track.albumId,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isActive) LocalDynamicColors.current.accent else appColors.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = appColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        val btnInteraction = remember { MutableInteractionSource() }
        val accent = LocalDynamicColors.current.accent
        val isCurrentlyPlaying = isActive && isPlaying
        val iconVector = when {
            action == TrackAction.FAVORITE -> Icons.Rounded.Favorite
            isCurrentlyPlaying -> Icons.Rounded.Pause
            else -> Icons.Rounded.PlayArrow
        }

        Box(
            modifier = Modifier
                .pressScale(btnInteraction, pressedScale = 0.88f)
                .clip(CircleShape)
                .background(if (isCurrentlyPlaying) accent else appColors.bgGlass)
                .clickable(btnInteraction, indication = null) {
                    haptic()
                    onPlay()
                }
                .size(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = if (isCurrentlyPlaying) "Pause" else "Play",
                tint = if (isCurrentlyPlaying) Color.White else if (action == TrackAction.FAVORITE) accent else appColors.textPrimary,
                modifier = Modifier.size(if (action == TrackAction.FAVORITE) 15.dp else 17.dp),
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun RankedTrackRow(
    played: PlayedTrack,
    rank: Int,
    isActive: Boolean,
    isPlaying: Boolean = false,
    onPlay: () -> Unit,
    onLongClick: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .pressScale(interaction, pressedScale = 0.96f)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isActive) {
                    Brush.linearGradient(
                        listOf(
                            LocalDynamicColors.current.accent.copy(alpha = 0.22f),
                            appColors.bgGlass,
                        ),
                    )
                } else {
                    Brush.linearGradient(listOf(appColors.bgGlass, appColors.bgGlass))
                },
            )
            .border(
                1.dp,
                if (isActive) LocalDynamicColors.current.accent.copy(alpha = 0.4f) else appColors.borderGlass.copy(alpha = 0.65f),
                RoundedCornerShape(18.dp),
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptic()
                    onPlay()
                },
                onLongClick = {
                    haptic()
                    onLongClick()
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = rank.toString(),
            color = if (rank == 1) Color(0xFF6366F1) else appColors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(22.dp),
        )
        AlbumArt(
            albumId = played.track.albumId,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = played.track.title,
                color = if (isActive) LocalDynamicColors.current.accent else appColors.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${played.track.displayArtist}  •  ${played.playCount} ${if (played.playCount == 1) "play" else "plays"}",
                color = appColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        val btnInteraction = remember { MutableInteractionSource() }
        val accent = LocalDynamicColors.current.accent
        val isCurrentlyPlaying = isActive && isPlaying

        Box(
            modifier = Modifier
                .pressScale(btnInteraction, pressedScale = 0.88f)
                .clip(CircleShape)
                .background(if (isCurrentlyPlaying) accent else appColors.bgGlass)
                .clickable(btnInteraction, indication = null) {
                    haptic()
                    onPlay()
                }
                .size(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isCurrentlyPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isCurrentlyPlaying) "Pause" else "Play",
                tint = if (isCurrentlyPlaying) Color.White else appColors.textPrimary,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MostPlayedScreen(
    app: AppViewModel,
    ui: HomeUiState.Success,
    playback: com.gorilla.music.playback.PlaybackState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    BackHandler {
        onBack()
    }

    val appColors = LocalAppColors.current
    val playedTracks = ui.mostPlayed.take(20)
    val queue = remember(playedTracks) { playedTracks.map { it.track } }
    val backPillColor = appColors.bgSurface.copy(alpha = 0.94f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.bgBase),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        ) {
            Column {
                Text(
                    text = "LISTENING TELEMETRY",
                    color = LocalDynamicColors.current.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                )
                Text(
                    text = "Top Played Tracks",
                    color = appColors.textPrimary,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .instantClickable(pressedScale = 0.90f) {
                        onBack()
                    }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(backPillColor, backPillColor)))
                    .border(1.dp, appColors.borderGlass, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = appColors.textPrimary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "Back",
                        color = appColors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            item {
                SectionHeader(
                    title = "Top Local Frequency",
                    action = "${playedTracks.size} Tracks",
                )
            }

            if (playedTracks.isEmpty()) {
                item { InlineEmptyMessage("Play counts will build as you listen.") }
            } else {
                items(
                    items = playedTracks,
                    key = { "played-${it.track.id}" },
                ) { played ->
                    val isActive = playback.current?.id == played.track.id
                    RankedTrackRow(
                        played = played,
                        rank = playedTracks.indexOf(played) + 1,
                        isActive = isActive,
                        isPlaying = isActive && playback.isPlaying,
                        onPlay = {
                            if (isActive) {
                                app.togglePlayPause()
                            } else {
                                app.playTrack(played.track, queue)
                            }
                        },
                        onLongClick = { app.openTrackMenu(played.track, queue) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineEmptyMessage(message: String) {
    val appColors = LocalAppColors.current
    Text(
        text = message,
        color = appColors.textSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
    )
}

private fun formatListeningTime(milliseconds: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.coerceAtLeast(0L))
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 99 -> "${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
