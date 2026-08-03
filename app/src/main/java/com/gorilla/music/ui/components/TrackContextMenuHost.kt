package com.gorilla.music.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorilla.music.data.model.Track
import com.gorilla.music.ui.AppViewModel

/**
 * Hosts the [TrackContextSheet] and wires its actions to the existing [AppViewModel]
 * intents. [context] is the surrounding track list so "Play now" keeps next/prev intact.
 */
@Composable
fun TrackContextMenuHost(
    app: AppViewModel,
    menuTrack: Track?,
    onDismiss: () -> Unit,
    context: List<Track> = emptyList(),
) {
    val playlists by app.playlists.collectAsStateWithLifecycle()

    menuTrack?.let { track ->
        TrackContextSheet(
            track = track,
            playlists = playlists,
            onPlayNow = { app.playTrack(track, context.ifEmpty { listOf(track) }) },
            onPlayNext = { app.playNext(track) },
            onAddToQueue = { app.addToQueue(track) },
            onAddToPlaylist = { playlistId -> app.addTrackToPlaylist(playlistId, track.id) },
            onNewPlaylist = { name -> app.createPlaylistWithTrack(name, track.id) },
            onEqualizer = { app.openEqualizer() },
            onTrackInfo = { app.openTrackInfo(track) },
            onEditTags = { app.openEditTags(track) },
            onDelete = { app.deleteTrack(track) },
            onDismiss = onDismiss,
        )
    }
}
