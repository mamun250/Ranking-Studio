package com.rankingstudio.app.ui.screens.editor.timeline

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object TimelineThumbnailCache {
    // 32MB cache for filmstrip frame bitmaps
    private val memoryCache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    suspend fun getFrameThumbnail(
        context: Context,
        videoUriString: String,
        timeUs: Long,
        targetWidth: Int = 120,
        targetHeight: Int = 120
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${videoUriString}_${timeUs / 500_000}_${targetWidth}x${targetHeight}"
        memoryCache.get(cacheKey)?.let { return@withContext it }

        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            if (videoUriString.startsWith("content://") || videoUriString.startsWith("file://")) {
                retriever.setDataSource(context, Uri.parse(videoUriString))
            } else {
                val file = File(videoUriString)
                if (file.exists()) {
                    retriever.setDataSource(file.absolutePath)
                } else {
                    retriever.setDataSource(context, Uri.parse(videoUriString))
                }
            }

            val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (bitmap != null) {
                val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                memoryCache.put(cacheKey, scaled)
                return@withContext scaled
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }
        return@withContext null
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
