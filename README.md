# GorillaMusic

A production-quality offline Android music player built with Kotlin + Jetpack Compose,
Media3 (ExoPlayer), Room, DataStore, and an Android 12+ Liquid Glass rendering
system adapted from Echo Music. Clean MVVM + Repository architecture.

## Opening the project

1. Open the `GorillaMusic` folder in **Android Studio** (Ladybug or newer).
2. The Gradle **wrapper jar + `gradlew` scripts are included** in the repository.
   You can build the project directly using `./gradlew` without needing to regenerate them.
3. Create `local.properties` with your SDK path if Android Studio doesn't (it usually
   does): `sdk.dir=/path/to/Android/Sdk`
4. Build & run on a device/emulator running **Android 12 (API 31) or newer**.
5. Grant the audio permission when prompted, then the library scans automatically.

## Requirements

- minSdk 31 (Android 12) — required for hardware `RenderEffect` blur.
- compileSdk 36 / targetSdk 35.
- JDK 17.

## Architecture

```
MediaPlaybackManager   singleton, owns the MediaController bound to PlaybackService
PlaybackService        foreground MediaSessionService hosting the real ExoPlayer
MusicRepository        MediaStore scan, MediaMetadataRetriever/MediaFormat, Room cache
PlaylistRepository     playlist CRUD + reorder in Room
SettingsRepository     all settings in DataStore
AppViewModel           global state: library, playback, palette, settings
Per-screen ViewModels  Library / Search / Playlists / TrackInfo / Settings
```

### Design system (`ui/theme/`)

- `LiquidGlassModifier.kt` — Echo Music's configurable blur, vibrancy, lens,
  highlight, shadow, tint, and adaptive-resolution recipe.
- `ui/liquidglass/backdrop/` — the modified Backdrop 2.0 renderer vendored by
  Echo Music.
- `LiquidGlassSurface.kt` — the shared surface component used by cards, sheets,
  navigation, and player chrome.
- `SpringSpec.kt` — all animation specs. The UI layer uses springs only (no
  `LinearEasing`).
- `Palette.kt` + `DynamicColors.kt` — album-art colors via the Palette API, propagated
  app-wide through `CompositionLocal` and animated on track change.

## Features

- **Home** — recently played (Room) + suggested shuffle, glass section headers.
- **Library** — Songs / Artists / Albums / Folders tabs, sort, swipe-to-queue.
- **Now Playing** — palette-driven blurred art background, glass control panel wired to
  Media3, drag-to-seek, swipe-down to mini player, swipe-up to Track Info.
- **Track Info sheet** — Format / Bitrate / Sample rate / Channels / Size / Duration /
  Encoding via `MediaMetadataRetriever` + `MediaFormat`.
- **Playlists** — create / rename / delete / add / remove / reorder, persisted in Room.
- **Search** — live debounced search with the expanding glass search bar.
- **Settings** — Appearance / Playback / Audio / About, every control wired to real
  state and applied immediately (blur intensity, accent, theme, crossfade, gapless,
  loudness, resume, rescan, etc.).

## Notes on scope

- **Crossfade** is implemented as a real volume ramp across track boundaries in
  `PlaybackService` (a true overlap-mixing crossfade would need dual players; the volume
  ramp is honest and audible). **Gapless** toggles `pauseAtEndOfMediaItems`. **Loudness
  normalization** applies a volume ceiling. All three genuinely change ExoPlayer
  behavior.
- Resume restores the last track + position (single-item queue) rather than the full
  prior queue — simplified deliberately rather than stubbed.
