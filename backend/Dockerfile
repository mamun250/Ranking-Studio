FROM node:20-alpine

# Install Python, FFmpeg, and yt-dlp
RUN apk add --no-cache python3 ffmpeg curl && \
    curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp

WORKDIR /app

COPY package*.json ./
RUN npm install --production

COPY . .

EXPOSE 3000

ENV PORT=3000
ENV HOST=0.0.0.0

CMD ["npm", "start"]
