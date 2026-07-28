# Project Name

Ranking Studio

## Goal

Build a professional Android application dedicated to creating Ranking Videos.

The UI should feel similar to CapCut/VN, but only include features required for ranking videos.

The application must be fast, smooth, modern, and production-ready.

No unnecessary editing tools.

---

# Platform

Android

Preferred Technology

- Native Kotlin
- Jetpack Compose
- Media3 (ExoPlayer)
- FFmpeg
- MVVM Architecture
- Material Design 3

Minimum Android Version: Android 9+

---

# Main Features

## Import Videos

Support:

- Gallery Import
- TikTok Link Import

Example:

User pastes a TikTok URL.

The app downloads the video automatically.

The downloaded video is added directly to the timeline.

---

# TikTok Import

Create a backend API dedicated only for TikTok.

Flow

Android App

↓

Paste TikTok URL

↓

Backend API

↓

Download highest available quality

↓

Convert to MP4 if needed

↓

Return video

↓

Automatically import into timeline

Requirements

- Download progress
- Error handling
- Retry
- Queue support
- Fast response
- Temporary file cleanup
- Production ready

API

POST /import

Request

{
"url":"https://www.tiktok.com/..."
}

Response

{
"success":true,
"video":"..."
}

---

# Editor

CapCut-style interface.

Top Bar

- Project Name
- Export
- Undo
- Redo
- Settings

Center

Large 1080×1920 Preview

Live Preview

Play

Pause

Seek

Bottom

Timeline

Supports

- 1–7 clips
- Drag
- Trim
- Split
- Delete
- Replace
- Reorder

---

# Ranking Sidebar

Exactly like viral Ranking Videos.

Show

1

2

3

4

5

6

7

Each row contains

- Number
- Title
- Emoji

Current playing clip automatically highlights.

User can edit

- Font
- Color
- Size
- Stroke
- Shadow
- Glow
- Position
- Emoji

---

# Auto Sync

Timeline automatically updates the highlighted rank.

Clip 1

Rank 1 highlighted

Clip 2

Rank 2 highlighted

...

Clip 7

Rank 7 highlighted

No manual work required.

---

# Header

Three editable text lines.

Example

Ranking

Funniest

Parkour Moments

Each line supports

- Font
- Color
- Gradient
- Shadow
- Stroke
- Animation

---

# Progress Bar

Animated progress bar.

Editable

- Thickness
- Color
- Position

---

# Auto Number Animation

Whenever active rank changes

Animate

- Pop
- Scale
- Glow
- Bounce

---

# Watermark

Optional PNG watermark

Opacity

Scale

Position

Rotation

---

# Export

1080×1920

30 FPS

60 FPS

MP4

High Quality

Background Export

Progress Screen

---

# Save Projects

Create

Save

Open

Rename

Duplicate

Delete

---

# Templates

Save current design as template.

Reuse later with one click.

---

# Theme

Dark

Light

Automatic

---

# Backend

Node.js

Fastify

FFmpeg

Docker Ready

REST API

Queue System

Logging

Validation

Rate Limiting

Automatic Cleanup

---

# Folder Structure

Generate a clean production-ready architecture.

Separate

UI

Player

Timeline

Downloader

Exporter

API

Repository

Database

Utils

Models

---

# Documentation

Explain every file.

Explain every API.

Explain Android architecture.

Explain backend deployment.

Explain Docker deployment.

Explain VPS deployment.

Guide me step by step until the application is fully completed.

Never generate placeholder code.

Everything must be fully functional and production-ready.