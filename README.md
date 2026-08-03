<div align="center">
  <img src="https://raw.githubusercontent.com/RiderX9/GorillaMusic/main/icon.png" width="120" height="120" style="border-radius: 30px"/>
  
  # Gorilla Music
  
  A premium offline music player for Android with a Liquid Glass UI and deep audio metadata support.

  ![Android](https://img.shields.io/badge/Android-12%2B-green?style=flat-square&logo=android)
  ![Kotlin](https://img.shields.io/badge/Kotlin-purple?style=flat-square&logo=kotlin)
  ![Version](https://img.shields.io/badge/Version-1.0.0-blue?style=flat-square)
  ![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

  <br />

  [Download APK](https://github.com/RiderX9/GorillaMusic/releases/latest)
</div>

---

## Screenshots

| | | |
| :---: | :---: | :---: |
| <img src="https://github.com/user-attachments/assets/3df5137e-0a12-4c3a-ae12-8109d529856c" width="220" alt="Now Playing" /> | <img src="https://github.com/user-attachments/assets/db590538-bbd8-4c39-9446-41fcf8313a74" width="220" alt="Home" /> | <img src="https://github.com/user-attachments/assets/4cc09661-44db-44fd-954f-e419488839c6" width="220" alt="Library" /> |
| **Now Playing** | **Home** | **Library** |
| <img src="https://github.com/user-attachments/assets/53b14b00-c784-468d-9dd0-3dc9d88c4e1a" width="220" alt="Lyrics" /> | <img src="https://github.com/user-attachments/assets/05ba600b-10f3-4462-9175-1acf1552077b" width="220" alt="Browse" /> | <img src="https://github.com/user-attachments/assets/f06d62a9-6cf8-4183-b820-1deceeffefe3" width="220" alt="Tag Editor" /> |
| **Lyrics** | **Browse** | **Tag Editor** |

---

## Features

- 🦍 **Liquid Glass UI** — Real blur, specular highlights, and depth on every surface
- 🎨 **Reactive Background** — Animated gradient that pulls colors live from your album art
- 🎤 **Synced Lyrics** — Auto-fetched from LRCLIB with real-time line highlighting
- ✏️ **Tag Editor** — Edit title, artist, album, genre, year, and custom lyrics
- 📊 **Audio Quality Info** — Format, bitrate, and sample rate always visible
- ❤️ **Favourites** — Heart a song to instantly add it to your Favourites playlist
- 🔄 **Auto Library Scan** — New files appear automatically via ContentObserver
- 🎨 **Adaptive Accent Color** — Matches your album art or pick your own
- 📴 **Fully Offline** — No internet required for playback
- 🎵 **Formats** — MP3, FLAC, AAC, WAV, M4A, OGG, ALAC

---

## Tech Stack

| | |
|---|---|
| **Language** | Kotlin |
| **UI** | Jetpack Compose |
| **Playback** | Media3 / ExoPlayer |
| **Database** | Room |
| **Image Loading** | Coil |
| **Lyrics** | LRCLIB API |
| **Tag Editing** | JAudioTagger |

---

## Requirements

- Android 12 or higher
- Storage permission to scan local music files

---

## Installation

1. Go to [Releases](https://github.com/RiderX9/GorillaMusic/releases/latest)
2. Download the latest APK
3. Enable **Install from unknown sources** on your device
4. Install and enjoy

---

## Developer

Made by [@RiderX9](https://github.com/RiderX9)

---

## Acknowledgments & Open Source Libraries

This project is made possible by several incredible open-source libraries and projects. We would like to thank the creators and maintainers of the following:

- **[Echo Music](https://github.com/nyx-in/Echo-Music)** — For the Liquid Glass modifier, adaptive-resolution recipe, and Backdrop 2.0 renderer.
- **[AndroidX Media3 (ExoPlayer)](https://developer.android.com/media/media3)** — The core media playback engine.
- **[Jellyfin Media3 FFmpeg Decoder](https://github.com/jellyfin/jellyfin-media3)** — Audio decoding via FFmpeg extension (`org.jellyfin.media3:media3-ffmpeg-decoder`).
- **[JAudiotagger](https://bitbucket.org/ijabz/jaudiotagger/)** — Audio file metadata tagging and parsing (`net.jthink:jaudiotagger`).
- **[Coil](https://coil-kt.github.io/coil/)** — Image loading for Jetpack Compose.
- **[Timber](https://github.com/JakeWharton/timber)** — Logging utility.
- **[Accompanist](https://google.github.io/accompanist/)** — Jetpack Compose permission utilities.
- **[Material Kolor](https://github.com/Kyant0/MaterialKolor)** — Material You dynamic color generation.

*(Also includes functionality powered by `:youlyplus`, `:paxsenixlyrics`, and `:betterlyrics`)*
