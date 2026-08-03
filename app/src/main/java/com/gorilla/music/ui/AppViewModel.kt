package com.gorilla.music.ui

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.music.AppContainer
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.repo.MusicRepository
import com.gorilla.music.data.repo.PlaylistRepository
import com.gorilla.music.data.settings.AppSettings
import com.gorilla.music.data.settings.SettingsRepository
import com.gorilla.music.playback.MediaPlaybackManager
import com.gorilla.music.playback.PlaybackState
import com.gorilla.music.ui.theme.DynamicColors
import com.gorilla.music.ui.theme.paletteColorsFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.map

/** Whether the device music permission has been granted (drives gating UI). */
enum class LibraryStatus { NEEDS_PERMISSION, SCANNING, READY }

/** A pending system consent request (rename/delete shared media) for the Activity to launch. */
sealed interface ConsentRequest {
    data class Rename(val sender: android.content.IntentSender) : ConsentRequest
    data class Delete(val sender: android.content.IntentSender, val trackId: Long) : ConsentRequest
    data class EditTags(val sender: android.content.IntentSender, val trackId: Long) : ConsentRequest
}

/**
 * Global app state: the live library, playback state, the dynamic album-art palette,
 * and all settings. Per-screen ViewModels layer their own UI state on top of this.
 */
@OptIn(FlowPreview::class)
class AppViewModel(private val container: AppContainer) : ViewModel() {

    private val music: MusicRepository = container.musicRepository
    private val playlistsRepo: PlaylistRepository = container.playlistRepository
    private val settingsRepo: SettingsRepository = container.settingsRepository
    val playback: MediaPlaybackManager = container.playbackManager

    val tracks: StateFlow<List<Track>> = music.tracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isLibraryLoaded: StateFlow<Boolean> = music.tracks
        .map { true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val recentlyPlayed: StateFlow<List<Track>> = music.recentlyPlayed(20)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settings: StateFlow<AppSettings?> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val playbackState: StateFlow<PlaybackState> = playback.state

    private val _isDarkTheme = MutableStateFlow(false)
    fun updateDarkTheme(isDark: Boolean) { _isDarkTheme.value = isDark }

    private val _libraryStatus = MutableStateFlow(LibraryStatus.NEEDS_PERMISSION)
    val libraryStatus: StateFlow<LibraryStatus> = _libraryStatus.asStateFlow()

    private val _dynamicColors = MutableStateFlow(DynamicColors())
    val dynamicColors: StateFlow<DynamicColors> = _dynamicColors.asStateFlow()

    /** Whether the currently-playing track is favorited (drives the Now Playing heart). */
    val currentIsFavorite: StateFlow<Boolean> =
        kotlinx.coroutines.flow.combine(playbackState, tracks) { state, all ->
            val id = state.current?.id ?: return@combine false
            all.firstOrNull { it.id == id }?.isFavorite ?: state.current?.isFavorite ?: false
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** One-shot "Added N new tracks" events from the automatic incremental scan (N > 0 only). */
    private val _tracksAdded = kotlinx.coroutines.flow.MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val tracksAdded = _tracksAdded.asSharedFlow()

    private var permissionGranted = false
    private var resumeApplied = false
    private var autoScanRan = false

    private val statsPrefs =
        container.appContext.getSharedPreferences("listening_stats", android.content.Context.MODE_PRIVATE)
    private val radioPrefs =
        container.appContext.getSharedPreferences("radio_state", android.content.Context.MODE_PRIVATE)
    private val _lastRadioStationId = MutableStateFlow(
        if (radioPrefs.contains("last_station_id")) {
            radioPrefs.getLong("last_station_id", 0L)
        } else {
            null
        }
    )
    val lastRadioStationId: StateFlow<Long?> = _lastRadioStationId.asStateFlow()

    private val _actualListeningTimeMs = MutableStateFlow(statsPrefs.getLong("actual_listening_time_ms", 0L))
    val actualListeningTimeMs: StateFlow<Long> = _actualListeningTimeMs.asStateFlow()

    private val _listeningTimeTodayMs = MutableStateFlow(statsPrefs.getLong("listening_today_ms", 0L))
    val listeningTimeTodayMs: StateFlow<Long> = _listeningTimeTodayMs.asStateFlow()

    private val _listeningTimeYesterdayMs = MutableStateFlow(statsPrefs.getLong("listening_yesterday_ms", 0L))
    val listeningTimeYesterdayMs: StateFlow<Long> = _listeningTimeYesterdayMs.asStateFlow()

    init {
        playback.connect()

        playbackState
            .onEach { state ->
                val stationId = state.current?.takeIf { it.folder == "radio" }?.id ?: return@onEach
                if (_lastRadioStationId.value != stationId) {
                    _lastRadioStationId.value = stationId
                    radioPrefs.edit().putLong("last_station_id", stationId).apply()
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            var lastTickMs = System.currentTimeMillis()
            val todayDate = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
            val storedDate = statsPrefs.getString("listening_date", todayDate) ?: todayDate

            if (storedDate != todayDate) {
                val prevToday = statsPrefs.getLong("listening_today_ms", 0L)
                statsPrefs.edit()
                    .putString("listening_date", todayDate)
                    .putLong("listening_yesterday_ms", prevToday)
                    .putLong("listening_today_ms", 0L)
                    .apply()
                _listeningTimeYesterdayMs.value = prevToday
                _listeningTimeTodayMs.value = 0L
            }

            while (isActive) {
                kotlinx.coroutines.delay(1000)
                val now = System.currentTimeMillis()
                val elapsed = now - lastTickMs
                lastTickMs = now
                if (playbackState.value.isPlaying) {
                    val nextTotal = _actualListeningTimeMs.value + elapsed
                    val nextToday = _listeningTimeTodayMs.value + elapsed
                    _actualListeningTimeMs.value = nextTotal
                    _listeningTimeTodayMs.value = nextToday
                    statsPrefs.edit()
                        .putLong("actual_listening_time_ms", nextTotal)
                        .putLong("listening_today_ms", nextToday)
                        .apply()
                }
            }
        }

        // Observe real-time scans from ContentObserver and notify UI
        viewModelScope.launch {
            music.scanResults.collect { added ->
                if (added > 0) {
                    _tracksAdded.emit(added)
                }
            }
        }

        // Record plays + persist resume state + refresh palette whenever the track changes.
        playback.setOnTrackStarted { track ->
            if (track.id > 0L) {
                viewModelScope.launch {
                    music.recordPlay(track.id, nowMillis())
                }
            }
            refreshPaletteFor(track)
        }

        // Persist resume position periodically. Sampled to ~once every 5s so the
        // 500ms progress ticks don't hammer DataStore with two disk writes per second.
        playbackState
            .sample(5_000)
            .onEach { state ->
                state.current?.let { persistResume(it.id, state.positionMs) }
            }
            .launchIn(viewModelScope)

        // Apply audio + accent settings to the engine and palette whenever they change.
        settings
            .filterNotNull()
            .onEach { s ->
                playback.applyAudioSettings(
                    crossfadeSeconds = s.crossfadeSeconds,
                    gapless = s.gaplessPlayback,
                    loudnessNormalization = s.loudnessNormalization,
                    equalizerEnabled = s.equalizerEnabled,
                    equalizerBandGains = s.equalizerBandGains,
                    equalizerPreampDb = s.equalizerPreampDb,
                    bassBoostStrength = s.bassBoostStrength,
                    virtualizerStrength = s.virtualizerStrength,
                )
                // Re-tint with the new accent over the current art.
                val current = playbackState.value.current
                if (current != null) refreshPaletteFor(current)
                else _dynamicColors.value = _dynamicColors.value.copy(accent = s.accent.color)
            }
            .launchIn(viewModelScope)
    }

    // ---- Library lifecycle ----

    fun onPermissionResult(granted: Boolean) {
        permissionGranted = granted
        if (granted) ensureLibrary() else _libraryStatus.value = LibraryStatus.NEEDS_PERMISSION
    }

    /**
     * Show cached data immediately if present, then silently run an incremental scan in
     * the background to pick up new/changed files without blocking Home from rendering.
     * Only falls back to a full scan when the cache is empty (first launch).
     */
    fun ensureLibrary() {
        if (!permissionGranted) {
            _libraryStatus.value = LibraryStatus.NEEDS_PERMISSION
            return
        }
        viewModelScope.launch {
            if (music.isLibraryCached()) {
                _libraryStatus.value = LibraryStatus.READY
                maybeApplyResume()
                runAutoIncrementalScan()
            } else {
                rescan()
            }
        }
    }

    /**
     * Background incremental scan, run once per cold start (and again after a long
     * background gap — see [onAppResumed]). Never flips the library into a SCANNING state
     * or shows a spinner; surfaces a one-shot count only when it actually adds tracks.
     */
    private fun runAutoIncrementalScan() {
        if (autoScanRan) return
        autoScanRan = true
        viewModelScope.launch {
            val added = music.incrementalScan()
            if (added > 0) _tracksAdded.tryEmit(added)
        }
    }

    /** Called from the Activity when the app returns to the foreground after a gap. */
    fun onAppResumed(backgroundedForMs: Long) {
        if (!permissionGranted) return
        if (backgroundedForMs < 5 * 60 * 1000L) return
        viewModelScope.launch {
            val added = music.incrementalScan()
            if (added > 0) _tracksAdded.tryEmit(added)
        }
    }

    /** Toggle favorite for the currently-playing track and persist immediately. */
    fun toggleFavoriteCurrent() {
        val id = playbackState.value.current?.id ?: return
        viewModelScope.launch { music.toggleFavorite(id) }
    }

    /**
     * Favourites (or un-favourites) a whole album/artist. Only tracks that differ from
     * [favorite] are touched, so the Favourites playlist stays consistent.
     */
    fun setTracksFavorite(tracks: List<Track>, favorite: Boolean) {
        val changed = tracks.filter { it.isFavorite != favorite }
        if (changed.isEmpty()) return
        viewModelScope.launch { changed.forEach { music.toggleFavorite(it.id) } }
    }

    /** Force a full rescan (Settings → About → "Scan library again"). */
    fun rescan() {
        viewModelScope.launch {
            _libraryStatus.value = LibraryStatus.SCANNING
            music.scanLibrary()
            _libraryStatus.value = LibraryStatus.READY
            maybeApplyResume()
        }
    }

    private suspend fun maybeApplyResume() {
        if (resumeApplied) return
        val s = settings.value ?: return
        if (!s.resumeOnOpen || s.lastTrackId < 0) return
        val track = music.getTrack(s.lastTrackId) ?: return
        playback.prepareResume(track, s.lastPositionMs)
        // Apply default shuffle/repeat from settings on first ready.
        playback.setShuffle(s.defaultShuffle)
        playback.setRepeat(s.defaultRepeat)
        resumeApplied = true
    }

    private fun persistResume(trackId: Long, positionMs: Long) {
        viewModelScope.launch { settingsRepo.setResumeState(trackId, positionMs) }
    }

    // ---- Playback intents (used by all screens) ----

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        playback.playQueue(tracks, startIndex)
    }

    fun playTrack(track: Track, context: List<Track>) {
        val idx = context.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val list = context.ifEmpty { listOf(track) }
        playback.playQueue(list, idx)
    }

    fun shuffleAll() {
        val all = tracks.value
        if (all.isEmpty()) return
        playback.setShuffle(true)
        playback.playQueue(all.shuffled(), 0)
    }

    fun togglePlayPause() = playback.togglePlayPause()
    fun playNext(track: Track) {
        playback.playNext(track)
        showSnackbarMessage("Playing next: ${track.title}")
    }
    fun addToQueue(track: Track) {
        playback.addToQueue(track)
    }

    fun toggleAutoplay() {
        if (playbackState.value.autoplay) {
            playback.setAutoplay(false)
            return
        }
        val current = playbackState.value.current ?: return
        val queuedIds = playbackState.value.queue.mapTo(hashSetOf()) { it.id }
        val recommendations = tracks.value
            .asSequence()
            .filter { it.id != current.id && it.id !in queuedIds }
            .groupBy { candidate ->
                when {
                    current.genre != null && candidate.genre == current.genre -> 4
                    candidate.artist == current.artist -> 3
                    current.albumId > 0 && candidate.albumId == current.albumId -> 2
                    candidate.isFavorite -> 1
                    else -> 0
                }
            }
            .toSortedMap(reverseOrder())
            .values
            .flatMap { it.shuffled() }
        playback.setAutoplay(true, recommendations)
    }

    // ---- Track context-menu intents (rename / delete / playlists) ----

    val playlists: StateFlow<List<com.gorilla.music.data.db.PlaylistEntity>> =
        playlistsRepo.observePlaylists()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** One-shot requests that need an Activity to launch a system consent IntentSender. */
    private val _pendingConsent = kotlinx.coroutines.flow.MutableSharedFlow<ConsentRequest>(extraBufferCapacity = 4)
    val pendingConsent = _pendingConsent.asSharedFlow()

    /** A track + new title remembered while a rename consent dialog is in flight. */
    private var pendingRename: Pair<Track, String>? = null

    /** Create a new playlist and immediately add a track to it (context-menu "New playlist…"). */
    fun createPlaylistWithTrack(name: String, trackId: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = playlistsRepo.create(name.trim(), nowMillis())
            playlistsRepo.addTrack(id, trackId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { playlistsRepo.addTrack(playlistId, trackId) }
    }

    fun setPlaylistPinned(playlistId: Long, isPinned: Boolean) {
        viewModelScope.launch { playlistsRepo.setPinned(playlistId, isPinned) }
    }

    fun movePlaylist(playlistId: Long, direction: Int) {
        viewModelScope.launch { playlistsRepo.movePlaylist(playlistId, direction) }
    }

    /** Rename a track's MediaStore title; may raise a system consent request. */
    fun renameTrack(track: Track, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val sender = music.renameTrack(track, newTitle)
            if (sender != null) {
                pendingRename = track to newTitle.trim()
                _pendingConsent.emit(ConsentRequest.Rename(sender))
            }
        }
    }

    /** Called by the Activity once a rename consent request is approved. */
    fun onRenameConsentApproved() {
        val (track, title) = pendingRename ?: return
        pendingRename = null
        viewModelScope.launch { music.applyRename(track, title) }
    }

    /** Delete a track file from storage; always needs a system consent request. */
    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            val sender = music.buildDeleteRequest(listOf(track))
            _pendingConsent.emit(ConsentRequest.Delete(sender, track.id))
        }
    }

    /** Called by the Activity once a delete consent request is approved. */
    fun onDeleteConsentApproved(trackId: Long) {
        viewModelScope.launch { music.onDeleteConfirmed(trackId) }
    }

    /** A track + new tags remembered while a write consent dialog is in flight. */
    private var pendingTagEdit: PendingTagEdit? = null

    data class PendingTagEdit(
        val track: Track,
        val title: String?,
        val artist: String?,
        val album: String?,
        val genre: String?,
        val year: Int?,
        val lyrics: String?,
    )

    fun saveTags(
        track: Track,
        title: String?,
        artist: String?,
        album: String?,
        genre: String?,
        year: Int?,
        lyrics: String?
    ) {
        val tClean = title?.trim()?.takeIf { it.isNotBlank() }
        val aClean = artist?.trim()?.takeIf { it.isNotBlank() }
        val alClean = album?.trim()?.takeIf { it.isNotBlank() }
        val gClean = genre?.trim()?.takeIf { it.isNotBlank() }
        val yClean = year?.takeIf { it > 0 }
        val lClean = lyrics?.trim()?.takeIf { it.isNotBlank() }
        val lrcRegex = Regex("""^\[\d{2}:\d{2}\.\d{2,3}]""")
        val isSynced = lClean?.lineSequence()?.any { lrcRegex.containsMatchIn(it.trim()) } == true

        viewModelScope.launch {
            // Step 1: Write to Room immediately
            music.updateOverrides(track.id, tClean, aClean, alClean, gClean, yClean, lClean, isSynced)

            // Step 2: Build Write Request via MediaStore
            try {
                val pendingIntent = music.buildWriteRequest(track)
                pendingTagEdit = PendingTagEdit(track, tClean, aClean, alClean, gClean, yClean, lClean)
                _pendingConsent.emit(ConsentRequest.EditTags(pendingIntent.intentSender, track.id))
            } catch (e: Exception) {
                // If createWriteRequest fails, we still saved to Room
            }
        }
    }

    fun onEditTagsConsentApproved() {
        val edit = pendingTagEdit ?: return
        pendingTagEdit = null
        viewModelScope.launch {
            val success = music.writePhysicalTags(
                edit.track,
                edit.title,
                edit.artist,
                edit.album,
                edit.genre,
                edit.year
            )
            if (success) {
                music.rescanFile(edit.track) {
                    viewModelScope.launch {
                        music.incrementalScan()
                    }
                }
            }
        }
    }

    fun onEditTagsConsentDenied() {
        pendingTagEdit = null
        showSnackbarMessage("Tags saved in app only — file not modified")
    }

    // ---- Palette ----

    private fun refreshPaletteFor(track: Track) {
        val accent = (settings.value ?: AppSettings()).accent.color
        viewModelScope.launch {
            val colors = withContext(Dispatchers.IO) {
                val bytes = music.embeddedArt(track.uri)
                val bmp = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                paletteColorsFrom(bmp, accent)
            }
            _dynamicColors.value = colors
        }
    }

    /** Monotonic-ish wall clock; injected so repos stay time-free. */
    private fun nowMillis(): Long = System.currentTimeMillis()

    // ---- Global Sheets (Bug 1 Fix) ----

    private val _sheetTrack = MutableStateFlow<Track?>(null)
    val sheetTrack: StateFlow<Track?> = _sheetTrack.asStateFlow()

    private val _infoTrack = MutableStateFlow<Track?>(null)
    val infoTrack: StateFlow<Track?> = _infoTrack.asStateFlow()

    private val _lyricsTrack = MutableStateFlow<Track?>(null)
    val lyricsTrack: StateFlow<Track?> = _lyricsTrack.asStateFlow()

    private val _editTagsTrack = MutableStateFlow<Track?>(null)
    val editTagsTrack: StateFlow<Track?> = _editTagsTrack.asStateFlow()

    /** The equalizer sheet is global, not per-track, so a flag is enough. */
    private val _equalizerOpen = MutableStateFlow(false)
    val equalizerOpen: StateFlow<Boolean> = _equalizerOpen.asStateFlow()

    private val _showSnackbar = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 4)
    val showSnackbar = _showSnackbar.asSharedFlow()

    fun showSnackbarMessage(msg: String) {
        _showSnackbar.tryEmit(msg)
    }

    private var _sheetContext: List<Track> = emptyList()
    val sheetContext: List<Track> get() = _sheetContext

    private val _requestedLibraryTab = kotlinx.coroutines.flow.MutableStateFlow<com.gorilla.music.ui.screens.library.LibraryTab?>(null)
    val requestedLibraryTab = _requestedLibraryTab.asStateFlow()

    fun requestLibraryTab(tab: com.gorilla.music.ui.screens.library.LibraryTab?) {
        _requestedLibraryTab.value = tab
    }

    private val _requestedPlaylistId = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val requestedPlaylistId = _requestedPlaylistId.asStateFlow()

    fun requestPlaylist(id: Long?) {
        _requestedPlaylistId.value = id
    }

    fun moveFolder(folderPath: String, up: Boolean, currentFolders: List<com.gorilla.music.data.model.Folder>) {
        viewModelScope.launch {
            val currentOrder = settings.value?.customFolderOrder?.takeIf { it.isNotEmpty() } 
                ?: currentFolders.map { it.path }
            val index = currentOrder.indexOf(folderPath)
            if (index == -1) return@launch
            val newIndex = if (up) index - 1 else index + 1
            if (newIndex !in currentOrder.indices) return@launch

            val mutableOrder = currentOrder.toMutableList()
            mutableOrder.removeAt(index)
            mutableOrder.add(newIndex, folderPath)
            settingsRepo.setCustomFolderOrder(mutableOrder)
        }
    }

    fun removeFolder(folderPath: String) {
        viewModelScope.launch {
            val hidden = settings.value?.hiddenFolders ?: emptyList()
            if (!hidden.contains(folderPath)) {
                settingsRepo.setHiddenFolders(hidden + folderPath)
            }
        }
    }

    fun openTrackMenu(track: Track, context: List<Track> = emptyList()) {
        _sheetContext = context
        _sheetTrack.value = track
    }

    fun closeTrackMenu() {
        _sheetTrack.value = null
    }

    fun openTrackInfo(track: Track) {
        _infoTrack.value = track
    }

    fun closeTrackInfo() {
        _infoTrack.value = null
    }

    fun openEqualizer() {
        _equalizerOpen.value = true
    }

    fun closeEqualizer() {
        _equalizerOpen.value = false
    }

    fun openLyrics(track: Track) {
        _lyricsTrack.value = track
    }

    fun closeLyrics() {
        _lyricsTrack.value = null
    }

    fun openEditTags(track: Track) {
        _editTagsTrack.value = track
    }

    fun closeEditTags() {
        _editTagsTrack.value = null
    }

    companion object {
        val Factory = viewModelFactory { container -> AppViewModel(container) }
    }
}
