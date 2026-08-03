package com.gorilla.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.ConsentRequest
import com.gorilla.music.ui.GorillaRoot
import com.gorilla.music.ui.theme.GorillaTheme
import com.gorilla.music.ui.theme.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {

    private var backgroundedAt: Long = 0L
    private var activeAppViewModel: AppViewModel? = null

    override fun onStop() {
        super.onStop()
        backgroundedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundedAt > 0L) {
            val gap = System.currentTimeMillis() - backgroundedAt
            activeAppViewModel?.onAppResumed(gap)
            backgroundedAt = 0L
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
            activeAppViewModel = appViewModel
            val settings by appViewModel.settings.collectAsStateWithLifecycle()
            val dynamic by appViewModel.dynamicColors.collectAsStateWithLifecycle()
            val loadedSettings = settings
            if (loadedSettings == null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
                return@setContent
            }

            // Launch system consent dialogs for rename / delete / edit tags of shared media, then
            // commit the change to the cache once the user approves.
            var pendingTrackId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(-1L) }
            var renameInFlight by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            var editTagsInFlight by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            val consentLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    if (renameInFlight) appViewModel.onRenameConsentApproved()
                    if (pendingTrackId >= 0) appViewModel.onDeleteConsentApproved(pendingTrackId)
                    if (editTagsInFlight) appViewModel.onEditTagsConsentApproved()
                } else {
                    if (editTagsInFlight) appViewModel.onEditTagsConsentDenied()
                }
                renameInFlight = false
                editTagsInFlight = false
                pendingTrackId = -1L
            }

            LaunchedEffect(Unit) {
                appViewModel.pendingConsent.collect { req ->
                    when (req) {
                        is ConsentRequest.Rename -> {
                            renameInFlight = true
                            consentLauncher.launch(IntentSenderRequest.Builder(req.sender).build())
                        }
                        is ConsentRequest.Delete -> {
                            pendingTrackId = req.trackId
                            consentLauncher.launch(IntentSenderRequest.Builder(req.sender).build())
                        }
                        is ConsentRequest.EditTags -> {
                            editTagsInFlight = true
                            consentLauncher.launch(IntentSenderRequest.Builder(req.sender).build())
                        }
                    }
                }
            }

            val isDark = when (loadedSettings.themeMode) {
                ThemeMode.AUTO -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.AMOLED -> true
            }
            appViewModel.updateDarkTheme(isDark)
            val accentColor = loadedSettings.accent.color
            // Dynamic Theme: seed the whole scheme from the current album artwork,
            // falling back to the chosen accent until art has been extracted.
            val seedColor = if (loadedSettings.dynamicTheme) {
                dynamic.themeSeed ?: accentColor
            } else {
                accentColor
            }
            val resolvedDynamic = dynamic.copy(
                accent = seedColor,
                artPrimary = if (dynamic.artPrimary == loadedSettings.accent.color) seedColor else dynamic.artPrimary
            )

            GorillaTheme(
                themeMode = loadedSettings.themeMode,
                seedColor = seedColor,
                accentChoice = loadedSettings.accent,
                blurIntensity = loadedSettings.blurIntensity,
                liquidGlassEnabled = loadedSettings.liquidGlassEnabled,
                liquidGlassIntensity = loadedSettings.liquidGlassIntensity,
                dynamicColors = resolvedDynamic,
            ) {
                GorillaRoot(appViewModel)
            }
        }
    }
}
