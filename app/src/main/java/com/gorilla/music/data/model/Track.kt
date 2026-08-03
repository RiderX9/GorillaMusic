package com.gorilla.music.data.model

import android.net.Uri

/** Immutable domain model for a single audio track. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val data: String,
    val folder: String,
    val mimeType: String,
    val size: Long,
    val dateAddedSec: Long,
    val trackNumber: Int,
    val year: Int,
    val isFavorite: Boolean = false,
    val genre: String? = null,
    val customLyrics: String? = null,
    val isSynced: Boolean = false,
    val overrideTitle: String? = null,
    val overrideArtist: String? = null,
    val overrideAlbum: String? = null,
    val overrideYear: Int? = null,
    val artworkUri: Uri? = null,
) {
    val displayArtist: String get() = artist.ifBlank { "Unknown artist" }
    val displayAlbum: String get() = album.ifBlank { "Unknown album" }

    /**
     * Format label for the track: the file extension when there is one, else
     * the MIME subtype. Note that [MusicRepository.detectActualFormat] stores
     * the literal "ALAC" in [mimeType] for ALAC-in-MP4, which the `.m4a`
     * extension cannot distinguish from lossy AAC.
     */
    val audioFormat: String
        get() = data.substringAfterLast('.', "").uppercase()
            .ifBlank { mimeType.substringAfterLast('/', "").uppercase() }

    /**
     * True when the track is stored in a lossless codec — drives the player's
     * quality pill. Accord keys its pill off `mimeType.contains("flac")`
     * alone; Gorilla already recognises the wider ALAC/WAV/AIFF/APE/DSD set in
     * the track-info sheet and the browse screen, so the pill follows that.
     */
    val isLossless: Boolean
        get() = audioFormat in LOSSLESS_FORMATS || mimeType.uppercase() in LOSSLESS_FORMATS
}

/** Container/codec labels that imply lossless audio, matched case-insensitively. */
val LOSSLESS_FORMATS = setOf(
    "FLAC", "X-FLAC",
    "ALAC", "X-ALAC",
    "WAV", "X-WAV", "WAVE",
    "AIFF", "AIF",
    "APE", "DSD", "DSF", "DFF",
)

/** Detailed technical metadata for the Track Info sheet (extracted on demand). */
data class TrackTechInfo(
    val format: String,
    val codec: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val channels: Int,
    val sizeBytes: Long,
    val durationMs: Long,
    val encoding: String,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val firstTrackId: Long,
)

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val representativeAlbumId: Long,
    val artworkUri: Uri?,
)

data class Folder(
    val path: String,
    val name: String,
    val trackCount: Int,
)
