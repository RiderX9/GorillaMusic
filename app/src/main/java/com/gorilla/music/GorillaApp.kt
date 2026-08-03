package com.gorilla.music

import android.app.Application
import com.gorilla.music.data.db.AppDatabase
import com.gorilla.music.data.repo.LyricsRepository
import com.gorilla.music.data.repo.MusicRepository
import com.gorilla.music.data.repo.PlaylistRepository
import com.gorilla.music.data.repo.RadioRepository
import com.gorilla.music.data.settings.SettingsRepository
import com.gorilla.music.playback.MediaPlaybackManager

/**
 * Application-scoped dependency container (manual DI — no Hilt needed for this size).
 * Exposed through [container] and consumed by ViewModel factories.
 */
class GorillaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: GorillaApp) {
    val appContext = app.applicationContext
    private val database = AppDatabase.get(app)

    val musicRepository = MusicRepository(app, database)
    val playlistRepository = PlaylistRepository(database)
    val radioRepository = RadioRepository()
    val settingsRepository = SettingsRepository(app)
    val lyricsRepository = LyricsRepository(app, database, musicRepository)
    val playbackManager = MediaPlaybackManager.get(app).apply {
        setLyricsRepository(lyricsRepository)
    }
}
