# Changelog

All notable changes to **Seal Next** are documented in this file.

Seal Next continues the lineage of [Seal](https://github.com/JunkFood02/Seal) (by
JunkFood02) and [Seal Plus](https://github.com/MaheshTechnicals/Sealplus) (by Mahesh
Technicalss). For the full history before v1.0.0, see the upstream
[Seal changelog](https://github.com/JunkFood02/Seal/blob/main/CHANGELOG.md) and
[Seal Plus changelog](https://github.com/MaheshTechnicals/Sealplus/blob/main/CHANGELOG.md).

## [1.3.0] – 2026-11-19

The stability release — eight user-reported bugs fixed, developed and maintained by **Md Khademul Islam**.

### 🧹 Fetch & Home
* **Clipboard auto-fetch removed** — the app no longer reads the clipboard on open/resume and the auto-detect banner is gone; paste only happens when you tap paste
* **Fetch deadlock fixed** — a hung fetch used to pin the modal sheet forever and the whole screen stopped responding. Fetches now have a hard 60 s deadline, shared links can't re-open the sheet out of nowhere, a navigation hijack while the download dialog was open was removed, and stale format/playlist pages reset on close

### 📥 Downloads
* **Facebook & other tricky sites** — when a fetch fails with an extraction error (public Facebook reels included), the app now silently updates the yt-dlp engine once and retries before showing an error
* **No more zombie processes** — deleting an active download now cancels its yt-dlp/aria2c process first (with a safety net in the engine), so nothing keeps burning RAM after the task is gone

### ▶️ Player
* **Fullscreen crash fixed** — the inline and fullscreen views both attached to the same ExoPlayer and the fullscreen dialog thrashed screen orientation on every aspect change; now there is exactly one player view and one orientation decision per fullscreen session, and vertical videos no longer get forced landscape
* **Brightness gesture fixed** — it now targets the window that actually shows the player (the fullscreen dialog window), vertical drags no longer lose the gesture race to the seek detector, and the brightness override resets when the player closes
* **Stuck loading fixed** — stream resolution has a 45 s deadline with network-level call timeouts, so the player never hangs on "loading" forever
* **Quick Discover chips removed**; YouTube search now clears stale results, shows a clear "no results" message, and has the same timeout protection

### 🔋 Memory & Performance
* **Device-hang fixes** — a Wi-Fi reconnect no longer spawns every queued download at once (staggered + respects the network preference), a yt-dlp update no longer spins up the foreground service, a low-memory callback no longer permanently kills the download engine, and the v1 progress pipeline is throttled (200 ms UI / 750 ms notification) like the v2 engine
* Trim-dialog video duration is now read off the main thread; per-line download logging is throttled

## [1.2.0] – 2026-11-18

The self-updating release, developed and maintained by **Md Khademul Islam**.

### 🔄 In-App Update System
* **Auto-check on launch** — the app now checks GitHub for a newer stable release every time it starts and shows an update card with the full release notes
* **Manual check** — Settings → About → Check for Updates, with stable / pre-release channel selection and an auto-update toggle
* **One-tap download & install** — the update APK downloads with a live progress dialog and opens the installer straight from the app; the "install unknown apps" permission is requested once and later updates install with a single tap

### 🌐 Browser & Player
* **Browser history & bookmarks** — visited pages and saved sites in the built-in browser now persist between sessions
* **Picture-in-Picture** — the player supports PiP and survives rotation and window resizing without restarting playback
* **Playback resume** — the player remembers where you stopped and continues from there
* **Smarter YouTube extraction** — stream URLs are normalized and quality variants are built from the video's actual formats, with NewPipe falling back to the yt-dlp engine

### 🏠 Home & Polish
* Home cards, details dialogs and the glowing action button were refactored into dedicated components for a cleaner, snappier home screen
* Under-the-hood improvements to the ad-block filter, Wi-Fi queue manager, hidden-content vault and download pipeline

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

[1.3.0]: https://github.com/khad3m/Seal-Next/releases/tag/v1.3.0
[1.2.0]: https://github.com/khad3m/Seal-Next/releases/tag/v1.2.0
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
