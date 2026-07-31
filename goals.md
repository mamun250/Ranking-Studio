# EXPORT ENGINE (HIGHEST PRIORITY)

Video export is one of the most important features of this application.

The exported video must match the preview exactly.

No quality loss.

No frame drops.

No desynchronization.

No incorrect timing.

Implement a professional rendering engine.

## Export Requirements

Formats

- MP4 (H.264)
- MP4 (H.265 if supported)

Resolution

- 1080×1920 (Default)
- 720×1280 (Optional)

Frame Rate

- 30 FPS
- 60 FPS

Bitrate

- Auto
- 8 Mbps
- 12 Mbps
- 16 Mbps
- 20 Mbps

Encoding

Use hardware encoder whenever available.

Fallback to software encoder if necessary.

Support GPU acceleration.

Export should work on mid-range Android devices.

## Rendering

The rendered video must include

✔ Video clips

✔ Timeline edits

✔ Trimming

✔ Split clips

✔ Clip order

✔ Header text

✔ Ranking sidebar

✔ Active rank highlight

✔ Progress bar

✔ Watermark

✔ Animations

✔ Fonts

✔ Shadows

✔ Stroke

✔ Emojis

Everything shown in preview must appear in the exported video exactly.

## Export Screen

Show

Export progress

Remaining time

Current FPS

Encoding speed

Estimated file size

Pause export (optional)

Cancel export

Export completed screen

Open video

Share video

## Performance

Background export.

App should remain responsive while exporting.

Large projects should not crash.

Memory optimized.

Support videos longer than 30 minutes.

## File Quality

Maintain original source quality whenever possible.

Do not unnecessarily recompress imported videos.

Avoid visible compression artifacts.

Target professional-quality output suitable for TikTok, Instagram Reels, and YouTube Shorts.

## Architecture

Build a dedicated Export Engine.

Separate rendering logic from UI.

Use a clean and scalable architecture.

Do not use placeholder rendering code.

Implement a fully functional production-ready export pipeline.