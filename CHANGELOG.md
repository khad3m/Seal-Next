# Changelog

All notable changes to **Seal Next** are documented in this file.

Seal Next continues the lineage of [Seal](https://github.com/JunkFood02/Seal) (by
JunkFood02) and [Seal Plus](https://github.com/MaheshTechnicals/Sealplus) (by Mahesh
Technicalss). For the full history before v1.0.0, see the upstream
[Seal changelog](https://github.com/JunkFood02/Seal/blob/main/CHANGELOG.md) and
[Seal Plus changelog](https://github.com/MaheshTechnicals/Sealplus/blob/main/CHANGELOG.md).

## [1.1.0] – 2026-11-18

The premium experience release, developed and maintained by **Md Khademul Islam**.

### 🎯 Premium Fetch Experience
* **New fetch sheet** — submitting a link now auto-fetches the media and shows a rich preview card (thumbnail, title, uploader, duration, views) before downloading
* **Preset chips & real quality grid** — video presets (Best → 360p), audio presets (Best → Low 64k) plus a grid mined from the video's actual available formats
* Playlist links are detected and offer a one-tap whole-playlist download

### 🛡️ Download Reliability
* **yt-dlp auto-updates in the background** (throttled to once a day) — a stale bundled extractor was the main cause of downloads failing after a while and of extraction errors listing "missing" pieces
* Quick-share downloads enqueue through the foreground-service pipeline before the share sheet closes

### ▶️ Solid Glassmorphism Player
* Brand-new player overlay: frosted-glass controls, auto-hiding chrome, blurred thumbnails on Android 12+
* **Full gestures** — tap to toggle controls, double-tap left/right to skip ±10s, horizontal swipe to scrub, vertical drag (left) for brightness, (right) for volume
* Speed picker dialog, live position bubble, buffered-position aware glass seekbar

### 🌐 PWA-Style Browser
* The browser no longer drops you straight into a webview — a **native launcher screen** now shows premium cards for YouTube, Instagram, TikTok and Facebook plus a manual address card
* Site cards open PWA-like full-screen browsing; the old always-visible Download / Watch / Copy bar is replaced by a **single floating circular button that expands** into the actions
* Back gesture: site → launcher → exit

### 🧠 Playback Fixes
* Browser engine now identifies as modern **Android Chrome** (better YouTube compatibility)
* **Dual-engine playback** — if NewPipe Extractor can't resolve a video, playback automatically falls back to the yt-dlp engine, fixing most YouTube "missing …" errors
* Friendly, actionable error messages instead of raw exception dumps

### 📦 Distribution
* **armeabi-v7a only** — every release now ships one 32-bit signed APK that installs on 32-bit and 64-bit devices alike

[1.1.0]: https://github.com/khad3m/Seal-Next/releases/tag/v1.1.0

## [1.0.0] – 2026-11-18

The first release of Seal Next — a fresh start for the Seal family, developed and
maintained by **Md Khademul Islam**.

### 🚀 Rebrand & Project

- **Seal Next** — new name, new identity, new home at [github.com/khad3m/Seal-Next](https://github.com/khad3m/Seal-Next)
- New application ID (`com.khad3m.sealnext`) — installs alongside older Seal/Seal Plus without conflicts
- **Reproducible signed builds** — the release keystore is committed to the repository, so every GitHub Actions build produces APKs that update in-place
- **New CI/CD pipeline** — signed release APKs are built automatically on every push to `main` and published to GitHub Releases (no external secrets required)
- Downloads, thumbnails and private folders now live under `Downloads/SealNext/`
- In-app update checker now tracks the Seal Next releases page
- Rebranded across the entire app: all 40+ supported interface languages
- MIT License

### 🔁 Retry All & Queue Recovery

* **Retry All on Home** — Retry All button in the "Recent downloads" header for Canceled or Failed tasks
* **Queued Tasks Survive Restarts** — Queued tasks restore as queued after force-stop instead of becoming Failed

### 🎬 Download Fixes

* **Custom Video Formats Fixed** — Custom formats with unreported codecs download the video correctly
* **MP4 Output for Merged Videos** — Video downloads use MP4 unless MKV is selected
* **Configure Switch Respected** — With "Configure before download" off, Quick Download, share, and the URL button download immediately
* **Output Template** — Custom templates no longer need `.%(ext)s`
* **Douyin Login** — Opens as a desktop session

### 🔋 Battery & Stability

* **Hide Battery Reminder** — Opt-out in General settings
* **HarmonyOS Crash Fixed** — Battery settings no longer crash on HarmonyOS 4.x
* **Smoother Progress** — Throttled progress updates fix unresponsive menus on OEM ROMs
* **Crash & Memory Hardening** — Safer download state handling and memory leak fixes

[1.0.0]: https://github.com/khad3m/Seal-Next/releases/tag/v1.0.0
