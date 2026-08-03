package com.gorilla.music.ui.components

import java.util.Locale
import java.util.concurrent.TimeUnit

/** mm:ss (or h:mm:ss) duration formatting. */
fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val h = TimeUnit.SECONDS.toHours(totalSec)
    val m = TimeUnit.SECONDS.toMinutes(totalSec) % 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

/** Human readable file size. */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unit = 0
    while (size >= 1024 && unit < units.lastIndex) {
        size /= 1024
        unit++
    }
    return String.format(Locale.US, "%.1f %s", size, units[unit])
}

fun formatSampleRate(hz: Int): String =
    if (hz <= 0) "—" else String.format(Locale.US, "%.1f kHz", hz / 1000.0)

fun formatBitrate(kbps: Int): String = if (kbps <= 0) "—" else "$kbps kbps"

fun formatChannels(channels: Int): String = when (channels) {
    0 -> "—"
    1 -> "Mono"
    2 -> "Stereo"
    else -> "$channels ch"
}
