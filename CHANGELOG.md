# Changelog

All notable changes to **Seal Next** are documented in this file.

Seal Next continues the lineage of [Seal](https://github.com/JunkFood02/Seal) (by
JunkFood02) and [Seal Plus](https://github.com/MaheshTechnicals/Sealplus) (by Mahesh
Technicalss). For the full history before v1.0.0, see the upstream
[Seal changelog](https://github.com/JunkFood02/Seal/blob/main/CHANGELOG.md) and
[Seal Plus changelog](https://github.com/MaheshTechnicals/Sealplus/blob/main/CHANGELOG.md).

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
