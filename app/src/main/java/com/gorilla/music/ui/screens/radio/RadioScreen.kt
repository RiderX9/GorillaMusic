package com.gorilla.music.ui.screens.radio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.gorilla.music.data.model.RadioStation
import com.gorilla.music.data.repo.toTrack
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.components.EmptyState
import com.gorilla.music.ui.components.LiquidGlassTabBar
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.accentBloom
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.rememberHaptic
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private val CapsuleShape = CircleShape

private enum class RadioRegion(val title: String) {
    EUROPE("Europe"),
    USA("USA"),
    BALKAN("Balkan"),
    WORLDWIDE("Worldwide"),
}

@Composable
fun RadioScreen(
    app: AppViewModel,
    contentPadding: PaddingValues,
    onOpenNowPlaying: () -> Unit,
    vm: RadioViewModel = viewModel(factory = RadioViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
    val playback by app.playbackState.collectAsStateWithLifecycle()
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val regions = RadioRegion.entries
    val pagerState = rememberPagerState(pageCount = { regions.size })
    val scope = rememberCoroutineScope()

    val allStations = (state as? RadioUiState.Success)?.stations ?: emptyList()

    // Spin animation for Refresh button
    val infiniteTransition = rememberInfiniteTransition(label = "refreshSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Box(Modifier.fillMaxSize()) {
        if (state is RadioUiState.Loading && allStations.isEmpty()) {
            Column(Modifier.fillMaxSize()) {
                HeaderSection(
                    accent = accent,
                    appColors = appColors,
                    isRefreshing = isRefreshing,
                    rotationAngle = rotationAngle,
                    onRefresh = vm::refresh,
                )
                EmptyState("Loading radio", "Finding online music stations.", icon = Icons.Rounded.GraphicEq)
            }
            return
        }

        if (state is RadioUiState.Error && allStations.isEmpty()) {
            Column(Modifier.fillMaxSize()) {
                HeaderSection(
                    accent = accent,
                    appColors = appColors,
                    isRefreshing = isRefreshing,
                    rotationAngle = rotationAngle,
                    onRefresh = vm::refresh,
                )
                EmptyState("Radio unavailable", (state as RadioUiState.Error).message, icon = Icons.Rounded.GraphicEq)
            }
            return
        }

        Column(Modifier.fillMaxSize()) {
            HeaderSection(
                accent = accent,
                appColors = appColors,
                isRefreshing = isRefreshing,
                rotationAngle = rotationAngle,
                onRefresh = vm::refresh,
            )
            RadioRegionTabs(
                regions = regions,
                pagerState = pagerState,
                onSelect = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
            )
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    pagerSnapDistance = PagerSnapDistance.atMost(1),
                ),
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val region = regions[page]
                val regionStations = remember(region, allStations) {
                    filterStationsForRegion(region, allStations)
                }
                val firstStation = regionStations.firstOrNull()
                val isHeroActive = firstStation != null && playback.current?.id == firstStation.id

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = contentPadding.calculateBottomPadding() + 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        RadioHeroCard(
                            region = region,
                            heroStation = firstStation,
                            isActive = isHeroActive,
                            isPlaying = isHeroActive && playback.isPlaying,
                            onTuneIn = {
                                if (firstStation != null) {
                                    app.playTrack(firstStation.toTrack(), regionStations.map { it.toTrack() })
                                }
                            },
                            onTogglePlayPause = { app.togglePlayPause() },
                        )
                    }
                    item {
                        RadioRegionHeading(
                            region = region,
                            stationCount = regionStations.size,
                            accent = accent,
                            appColors = appColors,
                        )
                    }
                    items(regionStations, key = { it.id }) { station ->
                        val isStationActive = playback.current?.id == station.id
                        RadioStationRowCard(
                            station = station,
                            isActive = isStationActive,
                            isPlaying = isStationActive && playback.isPlaying,
                            onClick = {
                                if (isStationActive) {
                                    app.togglePlayPause()
                                } else {
                                    app.playTrack(station.toTrack(), regionStations.map { it.toTrack() })
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun RadioRegionTabs(
    regions: List<RadioRegion>,
    pagerState: PagerState,
    onSelect: (Int) -> Unit,
) {
    LiquidGlassTabBar(
        labels = regions.map { it.title },
        selectedIndex = pagerState.currentPage,
        selectionPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction,
        onSelect = onSelect,
        compact = true,
    )
}

@Composable
private fun RadioRegionHeading(
    region: RadioRegion,
    stationCount: Int,
    accent: Color,
    appColors: com.gorilla.music.ui.theme.AppColors,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(
                text = when (region) {
                    RadioRegion.BALKAN -> "REGIONAL STREAM"
                    RadioRegion.EUROPE -> "EUROPEAN STREAM"
                    RadioRegion.USA -> "COAST TO COAST"
                    RadioRegion.WORLDWIDE -> "FEATURED GLOBAL"
                },
                color = appColors.textSecondary.copy(alpha = 0.60f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = when (region) {
                    RadioRegion.BALKAN -> "Balkan Stations"
                    RadioRegion.EUROPE -> "Popular Euro Stations"
                    RadioRegion.USA -> "Top US Channels"
                    RadioRegion.WORLDWIDE -> "Worldwide Favorites"
                },
                color = appColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Text(
            text = "All ($stationCount)",
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HeaderSection(
    accent: Color,
    appColors: com.gorilla.music.ui.theme.AppColors,
    isRefreshing: Boolean,
    rotationAngle: Float,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "ONLINE BROADCAST",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = "Radio",
                color = appColors.textPrimary,
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Row(
            modifier = Modifier
                .instantClickable(pressedScale = 0.90f) {
                    onRefresh()
                }
                .clip(CapsuleShape)
                .background(appColors.bgSurface.copy(alpha = 0.94f))
                .border(1.dp, appColors.borderGlass, CapsuleShape)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Rescan channels",
                tint = accent,
                modifier = Modifier
                    .size(15.dp)
                    .rotate(if (isRefreshing) rotationAngle else 0f),
            )
            Text(
                text = if (isRefreshing) "Scanning..." else "Refresh",
                color = appColors.textPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RadioHeroCard(
    region: RadioRegion,
    heroStation: RadioStation?,
    isActive: Boolean,
    isPlaying: Boolean,
    onTuneIn: () -> Unit,
    onTogglePlayPause: () -> Unit,
) {
    val (eyebrow, title, desc, defaultBtnText, bgColors, borderColor, btnTextColor) = when (region) {
        RadioRegion.EUROPE -> tupleOf(
            "EURO NETWORK",
            "European Frequencies",
            "Germany, France, UK & Club broadcasts",
            "Tune In",
            listOf(Color(0xFF1E1B4B), Color(0xFF312E81)),
            Color(0xFF6366F1).copy(alpha = 0.35f),
            Color(0xFF312E81),
        )
        RadioRegion.USA -> tupleOf(
            "US AIRWAVES",
            "USA Radio Network",
            "NYC, LA Hip-Hop & Coast to Coast FM",
            "Tune In",
            listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
            Color(0xFF38BDF8).copy(alpha = 0.35f),
            Color(0xFF0369A1),
        )
        RadioRegion.BALKAN -> tupleOf(
            "BALKAN STREAM",
            "Top Regional FM",
            "Live hits from Albania, Greece & Balkans",
            "Tune In",
            listOf(Color(0xFF4C0519), Color(0xFF881337)),
            Color(0xFFE11D48).copy(alpha = 0.35f),
            Color(0xFF881337),
        )
        RadioRegion.WORLDWIDE -> tupleOf(
            "GLOBAL SATELLITE",
            "Worldwide Stream",
            "Curated global streams from all continents",
            "Shuffle All",
            listOf(Color(0xFF1F2937), Color(0xFF111827)),
            Color(0xFF9CA3AF).copy(alpha = 0.35f),
            Color(0xFF111827),
        )
    }

    val cardShape = RoundedCornerShape(22.dp)
    val buttonText = if (isActive && isPlaying) "Pause" else defaultBtnText
    val buttonIcon = if (isActive && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Brush.linearGradient(bgColors))
            .border(1.dp, borderColor, cardShape)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow,
                color = Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = desc,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        Row(
            modifier = Modifier
                .padding(start = 12.dp)
                .instantClickable(pressedScale = 0.90f) {
                    if (isActive) {
                        onTogglePlayPause()
                    } else {
                        onTuneIn()
                    }
                }
                .clip(CapsuleShape)
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = buttonIcon,
                contentDescription = null,
                tint = btnTextColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = buttonText,
                color = btnTextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

private data class HeroTuple(
    val eyebrow: String,
    val title: String,
    val desc: String,
    val btnText: String,
    val bgColors: List<Color>,
    val borderColor: Color,
    val btnTextColor: Color,
)

private fun tupleOf(
    eyebrow: String,
    title: String,
    desc: String,
    btnText: String,
    bgColors: List<Color>,
    borderColor: Color,
    btnTextColor: Color,
) = HeroTuple(eyebrow, title, desc, btnText, bgColors, borderColor, btnTextColor)

@Composable
private fun RadioStationRowCard(
    station: RadioStation,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val shape = RoundedCornerShape(18.dp)
    val rowColor = appColors.bgSurface.copy(alpha = 0.94f)
    val background = if (isActive) {
        Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), rowColor))
    } else {
        Brush.linearGradient(listOf(rowColor, rowColor))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .instantClickable(pressedScale = 0.96f) {
                onClick()
            }
            .clip(shape)
            .background(background)
            .border(
                1.dp,
                if (isActive) accent.copy(alpha = 0.42f) else appColors.borderGlass,
                shape,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val artShape = RoundedCornerShape(12.dp)

        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(artShape),
            contentAlignment = Alignment.Center,
        ) {
            StationImage(station = station, modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 10.dp),
        ) {
            Text(
                text = station.name,
                color = if (isActive) accent else appColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = station.label,
                color = appColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        val actionIcon = if (isActive && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) accent.copy(alpha = 0.20f)
                    else Color.White.copy(alpha = 0.08f),
                )
                .border(
                    1.dp,
                    if (isActive) accent.copy(alpha = 0.50f) else appColors.borderGlass,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = if (isActive && isPlaying) "Pause station" else "Play station",
                tint = if (isActive) accent else Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun StationImage(station: RadioStation, modifier: Modifier = Modifier) {
    val imageUrl = station.favicon.takeIf { it.startsWith("http", ignoreCase = true) }
    val painter = rememberAsyncImagePainter(imageUrl)
    val painterState = painter.state

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        GeneratedStationArtwork(station = station)
        if (imageUrl != null && painterState !is AsyncImagePainter.State.Error && painterState !is AsyncImagePainter.State.Empty) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun GeneratedStationArtwork(station: RadioStation) {
    val accent = LocalDynamicColors.current.accent
    val colors = remember(station.id, accent) {
        val palette = listOf(
            Color(0xFF40C4FF),
            Color(0xFFFF2D55),
            Color(0xFFFFB020),
            Color(0xFF30D158),
            Color(0xFFBF5AF2),
            Color(0xFFFF6B4A),
        )
        val picked = palette[(station.id.absoluteValue % palette.size).toInt()]
        listOf(accent.copy(alpha = 0.88f), picked, Color(0xFF15151D))
    }
    val initials = remember(station.name) {
        station.name
            .split(' ', '-', '_', '.', '/', '|')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "GM" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.take(2),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

private val RadioStation.label: String
    get() = buildList {
        if (country.isNotBlank()) add(country)
        if (tags.isNotEmpty()) add(tags.first().replaceFirstChar { it.uppercase() })
        if (bitrate > 0) add("${bitrate}kbps")
    }.joinToString(" • ").ifBlank { codec.ifBlank { "Online Radio" } }

private val albanianStations = listOf(
    RadioStation(
        id = -1011L,
        name = "Top Albania Radio",
        streamUrl = "https://live.topalbania.al/stream",
        homepage = "https://topalbania.al",
        favicon = "https://www.google.com/s2/favicons?domain=topalbania.al&sz=256",
        tags = listOf("Pop", "Albania", "Hits"),
        country = "Albania",
        countryCode = "AL",
        bitrate = 128,
        codec = "MP3",
        hls = false,
        votes = 2500,
        clickCount = 1200,
    ),
    RadioStation(
        id = -1012L,
        name = "City Radio Albania",
        streamUrl = "https://stream.cityradio.al/live",
        homepage = "https://cityradio.al",
        favicon = "https://www.google.com/s2/favicons?domain=cityradio.al&sz=256",
        tags = listOf("Pop", "Dance", "Hits"),
        country = "Albania",
        countryCode = "AL",
        bitrate = 192,
        codec = "MP3",
        hls = false,
        votes = 1800,
        clickCount = 890,
    ),
    RadioStation(
        id = -1013L,
        name = "Club FM Tirana",
        streamUrl = "https://stream.clubfm.al/live",
        homepage = "https://clubfm.al",
        favicon = "https://www.google.com/s2/favicons?domain=clubfm.al&sz=256",
        tags = listOf("House", "Pop", "Albania"),
        country = "Albania",
        countryCode = "AL",
        bitrate = 128,
        codec = "MP3",
        hls = false,
        votes = 1650,
        clickCount = 750,
    ),
)

private fun filterStationsForRegion(
    region: RadioRegion,
    allStations: List<RadioStation>,
): List<RadioStation> {
    return when (region) {
        RadioRegion.EUROPE -> {
            val res = allStations.filter { s ->
                s.country.contains("Germany", true) ||
                    s.country.contains("France", true) ||
                    s.country.contains("UK", true) ||
                    s.country.contains("United Kingdom", true) ||
                    s.country.contains("Hungary", true) ||
                    s.country.contains("Italy", true) ||
                    s.country.contains("Spain", true) ||
                    s.country.contains("Netherlands", true) ||
                    s.country.contains("Poland", true) ||
                    s.country.contains("Austria", true) ||
                    s.country.contains("Switzerland", true) ||
                    s.countryCode in setOf("DE", "FR", "GB", "HU", "IT", "ES", "NL", "SE", "AT", "CH", "PL", "BE")
            }
            res.ifEmpty { allStations }
        }
        RadioRegion.USA -> {
            val res = allStations.filter { s ->
                s.country.contains("USA", true) ||
                    s.country.contains("United States", true) ||
                    s.country.contains("Canada", true) ||
                    s.countryCode in setOf("US", "CA")
            }
            res.ifEmpty { allStations }
        }
        RadioRegion.BALKAN -> {
            val balkanLive = allStations.filter { s ->
                s.country.contains("Albania", true) ||
                    s.country.contains("Greece", true) ||
                    s.country.contains("Serbia", true) ||
                    s.country.contains("Bulgaria", true) ||
                    s.country.contains("Croatia", true) ||
                    s.country.contains("Romania", true) ||
                    s.country.contains("Bosnia", true) ||
                    s.country.contains("Macedonia", true) ||
                    s.country.contains("Montenegro", true) ||
                    s.countryCode in setOf("AL", "GR", "RS", "BG", "HR", "RO", "BA", "MK", "ME", "XK")
            }
            (albanianStations + balkanLive).distinctBy { it.streamUrl }
        }
        RadioRegion.WORLDWIDE -> allStations.ifEmpty { albanianStations }
    }
}
