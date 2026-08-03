package com.gorilla.music.ui.screens.search

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.music.data.model.Track
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.components.AlbumArt
import com.gorilla.music.ui.components.EmptyState
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.accentBloom
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic
import com.gorilla.music.ui.theme.songCardColor

private enum class SearchScopeFilter(val label: String) {
    ALL("All Sources"),
    LOCAL("Local Storage"),
    PLAYLISTS("Playlists"),
}

private data class DynamicCategoryCard(
    val title: String,
    val subtitle: String,
    val query: String,
    val colors: List<Color>,
)

@Composable
fun SearchScreen(
    app: AppViewModel,
    contentPadding: PaddingValues,
    onOpenNowPlaying: () -> Unit,
    vm: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val allTracks by vm.allTracks.collectAsStateWithLifecycle()
    val playback by app.playbackState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var activeScopeFilter by remember { mutableStateOf(SearchScopeFilter.ALL) }
    val prefs = remember(context) {
        context.getSharedPreferences("search_history_prefs", android.content.Context.MODE_PRIVATE)
    }

    val historyItems = remember {
        val saved = prefs.getString("history", null)
        val list = saved?.split("|||")?.filter { it.isNotBlank() }?.take(6).orEmpty()
        mutableStateListOf<String>().apply { addAll(list) }
    }

    val addSearchQuery = remember(prefs, historyItems) {
        { rawQuery: String ->
            val q = rawQuery.trim()
            if (q.isNotBlank()) {
                historyItems.remove(q)
                historyItems.add(0, q)
                while (historyItems.size > 6) {
                    historyItems.removeAt(historyItems.lastIndex)
                }
                prefs.edit().putString("history", historyItems.joinToString("|||")).apply()
            }
        }
    }

    val removeSearchQuery = remember(prefs, historyItems) {
        { q: String ->
            historyItems.remove(q)
            prefs.edit().putString("history", historyItems.joinToString("|||")).apply()
        }
    }

    val clearHistory = remember(prefs, historyItems) {
        {
            historyItems.clear()
            prefs.edit().remove("history").apply()
        }
    }

    val dynamicCategories = remember(allTracks) {
        if (allTracks.isEmpty()) {
            emptyList()
        } else {
            val genreGroups = allTracks
                .filter { !it.genre.isNullOrBlank() }
                .groupBy { it.genre!!.trim() }
                .map { (genre, tracks) ->
                    DynamicCategoryCard(
                        title = genre,
                        subtitle = "${tracks.size} ${if (tracks.size == 1) "Local Track" else "Local Tracks"}",
                        query = genre,
                        colors = genrePalette(genre),
                    )
                }
                .sortedByDescending { it.subtitle }

            if (genreGroups.isNotEmpty()) {
                genreGroups.take(6)
            } else {
                allTracks
                    .groupBy { it.displayArtist }
                    .map { (artist, tracks) ->
                        DynamicCategoryCard(
                            title = artist,
                            subtitle = "${tracks.size} ${if (tracks.size == 1) "Local Track" else "Local Tracks"}",
                            query = artist,
                            colors = artistAvatarPalette(artist),
                        )
                    }
                    .sortedByDescending { it.subtitle }
                    .take(6)
            }
        }
    }

    val activeSearchPool = remember(query, allTracks, results, activeScopeFilter) {
        val base = if (query.isNotBlank()) results else allTracks
        when (activeScopeFilter) {
            SearchScopeFilter.ALL -> base
            SearchScopeFilter.LOCAL -> base.filter { it.size > 0 }
            SearchScopeFilter.PLAYLISTS -> base.filter { it.isFavorite }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.bgBase)
    ) {
        Column(Modifier.fillMaxSize()) {
            SearchHeader()
            SearchInputBar(
                query = query,
                onQueryChange = vm::onQueryChange,
                onClear = vm::clear,
                onSearchSubmit = {
                    addSearchQuery(query)
                },
            )
            SearchScopePills(
                selected = activeScopeFilter,
                onSelect = { filter ->
                    activeScopeFilter = filter
                },
            )

            Crossfade(
                targetState = query.isBlank() && activeScopeFilter == SearchScopeFilter.ALL,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "searchContentTransition",
            ) { isDefaultHome ->
                if (isDefaultHome) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            top = 4.dp,
                            end = 20.dp,
                            bottom = contentPadding.calculateBottomPadding() + 20.dp,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (historyItems.isNotEmpty()) {
                            item {
                                RecentSearchesHeader(
                                    onClearAll = clearHistory,
                                )
                            }
                            items(historyItems.take(6), key = { it }) { item ->
                                RecentSearchItem(
                                    queryText = item,
                                    onClick = {
                                        addSearchQuery(item)
                                        vm.onQueryChange(item)
                                    },
                                    onRemove = {
                                        removeSearchQuery(item)
                                    },
                                )
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp)
                                        .height(1.dp)
                                        .background(LocalAppColors.current.borderGlass),
                                )
                            }
                        }

                        if (dynamicCategories.isNotEmpty()) {
                            item {
                                ExploreCategoriesHeader()
                            }
                            item {
                                ExploreCategoriesGrid(
                                    categories = dynamicCategories,
                                    onCategoryClick = { category ->
                                        addSearchQuery(category.query)
                                        vm.onQueryChange(category.query)
                                    },
                                )
                            }
                        }
                    }
                } else {
                    if (activeSearchPool.isEmpty()) {
                        EmptyState(
                            title = "No results",
                            subtitle = if (query.isNotBlank()) "Nothing matches \"$query\" under ${activeScopeFilter.label}." else "No content found under ${activeScopeFilter.label}.",
                            icon = Icons.Rounded.SearchOff,
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                top = 4.dp,
                                end = 20.dp,
                                bottom = contentPadding.calculateBottomPadding() + 20.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            item {
                                Text(
                                    text = "SEARCH RESULTS (${activeSearchPool.size})",
                                    color = LocalDynamicColors.current.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
                                )
                            }
                            items(activeSearchPool, key = { it.id }) { track ->
                                SearchResultRow(
                                    track = track,
                                    isActive = playback.current?.id == track.id,
                                    onClick = {
                                        addSearchQuery(query)
                                        app.playTrack(track, activeSearchPool)
                                    },
                                    onQueue = {
                                        app.addToQueue(track)
                                        Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                                    },
                                    onMenu = {
                                        app.openTrackMenu(track, activeSearchPool)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader() {
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
                text = "LOCAL & STREAMS",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = "Search",
                color = appColors.textPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 38.sp,
            )
        }
    }
}

@Composable
private fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearchSubmit: () -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val barShape = RoundedCornerShape(20.dp)

    val borderColor by animateColorAsState(
        targetValue = if (query.isNotEmpty()) accent.copy(alpha = 0.48f) else appColors.borderGlass,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "searchBorderColor",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .height(52.dp)
            .clip(barShape)
            .background(appColors.bgSurface.copy(alpha = 0.94f))
            .border(1.dp, borderColor, barShape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Search icon",
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = appColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearchSubmit()
                        keyboardController?.hide()
                    },
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Songs, artists, albums, radio...",
                                color = appColors.textSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.75f, animationSpec = tween(160)),
                exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.75f, animationSpec = tween(160)),
            ) {
                val clearInteraction = remember { MutableInteractionSource() }
                val haptic = rememberHaptic()
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(appColors.bgGlass)
                        .clickable(clearInteraction, indication = null) {
                            haptic()
                            onClear()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchScopePills(
    selected: SearchScopeFilter,
    onSelect: (SearchScopeFilter) -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val haptic = rememberHaptic()

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
    ) {
        items(SearchScopeFilter.entries.toTypedArray(), key = { it.name }) { filter ->
            val isSelected = filter == selected
            val interaction = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .instantClickable(pressedScale = 0.94f) {
                        if (!isSelected) haptic()
                        onSelect(filter)
                    }
                    .accentBloom(accent, active = isSelected, shape = CircleShape)
                    .clip(CircleShape)
                    .background(if (isSelected) accent else appColors.bgSurface.copy(alpha = 0.92f))
                    .border(
                        1.dp,
                        if (isSelected) accent else appColors.borderGlass,
                        CircleShape,
                    )
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = filter.label,
                    color = if (isSelected) Color.White else appColors.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun RecentSearchesHeader(
    onClearAll: () -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(
                text = "HISTORY",
                color = appColors.textSecondary.copy(alpha = 0.60f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = "Recent Searches",
                color = appColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Text(
            text = "Clear",
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(interaction, indication = null) {
                    haptic()
                    onClearAll()
                }
                .padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun RecentSearchItem(
    queryText: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val removeInteraction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val itemShape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .instantClickable(pressedScale = 0.96f) {
                onClick()
            }
            .clip(itemShape)
            .background(appColors.bgSurface.copy(alpha = 0.90f))
            .border(1.dp, appColors.borderGlass, itemShape)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = appColors.textSecondary.copy(alpha = 0.60f),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = queryText,
                color = appColors.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .instantClickable(pressedScale = 0.90f) {
                    onRemove()
                }
                .size(24.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove item",
                tint = appColors.textSecondary.copy(alpha = 0.50f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ExploreCategoriesHeader() {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier.padding(bottom = 12.dp),
    ) {
        Text(
            text = "QUICK BROWSE",
            color = appColors.textSecondary.copy(alpha = 0.60f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
        )
        Text(
            text = "Explore Categories",
            color = appColors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun ExploreCategoriesGrid(
    categories: List<DynamicCategoryCard>,
    onCategoryClick: (DynamicCategoryCard) -> Unit,
) {
    val rowsCount = (categories.size + 1) / 2
    val gridHeight = (rowsCount * 112).dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
    ) {
        gridItems(categories, key = { it.title }) { category ->
            val interaction = remember { MutableInteractionSource() }
            val haptic = rememberHaptic()
            val cardShape = RoundedCornerShape(20.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .pressScale(interaction, pressedScale = 0.95f)
                    .clip(cardShape)
                    .background(Brush.linearGradient(category.colors))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), cardShape)
                    .clickable(interaction, indication = null) {
                        haptic()
                        onCategoryClick(category)
                    }
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = category.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = category.subtitle,
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    track: Track,
    isActive: Boolean,
    onClick: () -> Unit,
    onQueue: () -> Unit,
    onMenu: () -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val rowShape = RoundedCornerShape(16.dp)
    val rowColor = appColors.songCardColor()
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
            .clip(rowShape)
            .background(background)
            .border(
                1.dp,
                if (isActive) accent.copy(alpha = 0.42f) else appColors.borderGlass,
                rowShape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            albumId = track.albumId,
            artworkUri = track.artworkUri,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(52.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 4.dp),
        ) {
            Text(
                text = track.title,
                color = if (isActive) accent else appColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.displayArtist,
                color = appColors.textSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .instantClickable(pressedScale = 0.90f) {
                    onMenu()
                }
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (appColors.isDark) Color.White.copy(alpha = 0.06f)
                    else Color.Black.copy(alpha = 0.04f),
                )
                .border(1.dp, appColors.borderGlass, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MoreHoriz,
                contentDescription = "Track options",
                tint = appColors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun genrePalette(name: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFFFF007A), Color(0xFF7928CA)),
        listOf(Color(0xFF0070F3), Color(0xFF00DFD8)),
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
        listOf(Color(0xFF10B981), Color(0xFF059669)),
        listOf(Color(0xFF7C3AED), Color(0xFFDB2777)),
    )
    val index = (name.hashCode().toLong() and Long.MAX_VALUE) % palettes.size
    return palettes[index.toInt()]
}

private fun artistAvatarPalette(name: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)),
        listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
        listOf(Color(0xFF10B981), Color(0xFF059669)),
        listOf(Color(0xFFFF007A), Color(0xFF7928CA)),
    )
    val index = (name.hashCode().toLong() and Long.MAX_VALUE) % palettes.size
    return palettes[index.toInt()]
}
