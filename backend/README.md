# Ranking Studio - TikTok Import Backend API

Dedicated Node.js + Fastify API for processing and importing TikTok videos in highest quality for Ranking Studio Android App.

## Features
- **Fastify REST API**: Highly performant REST server.
- **`yt-dlp` Integration**: Downloads maximum available quality MP4 directly.
- **Rate Limiting**: Protects against spam (100 req/min).
- **Auto File Cleanup**: Automatically purges temporary downloaded files older than 1 hour.
- **Docker & Compose Support**: Single-command container deployment.

---

## API Endpoints

### 1. Import Video
- **URL**: `/import`
- **Method**: `POST`
- **Body**:
```json
{
  "url": "https://www.tiktok.com/@username/video/1234567890"
}
```
- **Response**:
```json
{
  "success": true,
  "video": "http://your-server-ip:3000/downloads/a1b2c3d4.mp4",
  "fileId": "a1b2c3d4.mp4",
  "message": "Video downloaded and processed successfully."
}
```

### 2. Health Check
- **URL**: `/health`
- **Method**: `GET`

---

## Deployment Guide

### Local Development
```bash
cd backend
npm install
npm run dev
```

### Docker / VPS Deployment
```bash
cd backend
docker-compose up -d --build
```
The service will be listening on port `3000`.
