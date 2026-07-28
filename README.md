# Ranking Studio

A professional Native Android application & Node.js backend dedicated to creating viral **1080×1920 Ranking Videos** (CapCut / VN style optimized for ranking list content).

Designed using Stitch's **Tactile Papercraft** design system.

---

## Features

### 🎬 Android App (Native Kotlin + Jetpack Compose)
- **Stitch Tactile Papercraft UI**: Textured cardstock buttons, sandish brown headers, paper white cards, olive green accents, and charcoal typography.
- **1080×1920 Live Preview Editor**:
  - Live ExoPlayer video canvas.
  - **Auto Sync**: Active rank item in the ranking sidebar automatically updates and highlights based on playback progress.
  - **Ranking Sidebar (1–7 Items)**: Number, title, emoji, custom fonts, colors, strokes, glows, and switch animations (pop, scale, bounce).
  - **3-Line Header Overlay**: Fully customizable titles.
  - **Progress Bar & Watermark Overlays**: Customizable thickness, opacity, scale, and position.
- **Timeline Editor**: Supports 1–7 clips with drag, trim, split, delete, replace, and reorder.
- **TikTok Import**: Instant import by pasting a TikTok video URL.
- **Video Export Engine**: Background export service via WorkManager & FFmpeg-Kit (30 FPS / 60 FPS 1080×1920 MP4).
- **Project & Template Storage**: Offline Room database for creating, renaming, duplicating, saving, and managing projects/templates.

### ⚡ TikTok Import Backend (`backend/`)
- **Fastify REST API**: `POST /import` endpoint.
- **yt-dlp Engine**: Automatically extracts highest available quality MP4.
- **Docker Ready**: `Dockerfile` & `docker-compose.yml` for instant deployment.
- **Auto Cleanup**: Background garbage collector purging files older than 1 hour.

### 🚀 CI/CD & Build Pipeline (`.github/workflows/`)
- **GitHub Actions (`android-build.yml`)**: Automatically compiles debug/release APKs and runs tests on GitHub push/PR.

---

## Directory Structure

```
Ranking Studio/
├── .github/
│   └── workflows/
│       └── android-build.yml       # GitHub Actions automated build script
├── app/                            # Android Application
│   ├── src/main/java/com/rankingstudio/app/
│   │   ├── data/                   # Room DB, Retrofit TikTok API, Repositories
│   │   ├── domain/                 # Project, Clip, Sidebar, Header models
│   │   ├── ui/
│   │   │   ├── components/         # Tactile Papercraft UI elements
│   │   │   ├── screens/            # Gallery & Ranking Editor screens
│   │   │   └── theme/              # Color, Type, Shape, Theme definitions
│   │   ├── exporter/               # FFmpeg-Kit VideoExporter & WorkManager
│   │   └── MainActivity.kt
│   └── build.gradle.kts
├── backend/                        # Node.js Fastify TikTok Downloader Server
│   ├── src/                        # Fastify server, routes, & downloader
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── package.json
│   └── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── prompt.md
└── README.md
```

---

## Quick Start

### 1. Run Node.js Backend API
```bash
cd backend
npm install
npm run dev
```
*(Or run with Docker: `docker-compose up -d --build`)*

### 2. Build Android App
Open root project in Android Studio or build via Gradle:
```bash
./gradlew assembleDebug
```

### 3. GitHub Actions Build
Push code to GitHub master/main branch. GitHub Actions will automatically run `./gradlew assembleDebug` and upload the compiled APK artifact!
