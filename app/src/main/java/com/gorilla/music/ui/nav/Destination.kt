package com.gorilla.music.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level navigation destinations shown in the GlassNavigationBar. */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "Home", Icons.Rounded.Home),
    Library("library", "Library", Icons.Rounded.LibraryMusic),
    Search("search", "Search", Icons.Rounded.Search),
    Browse("browse", "Browse", Icons.Rounded.GridView),
    Radio("radio", "Radio", Icons.Rounded.GridView),
    Playlists("playlists", "Playlists", Icons.AutoMirrored.Rounded.PlaylistPlay),
    Settings("settings", "Settings", Icons.Rounded.Settings);

    companion object {
        val bottomBar = listOf(Home, Browse, Search, Library, Settings)
        fun fromRoute(route: String?): Destination {
            if (route == Playlists.route || route == Radio.route) return Library
            return entries.firstOrNull { it.route == route } ?: Home
        }
    }
}

/** Non-tab routes. */
object Routes {
    const val NowPlaying = "now_playing"
}
