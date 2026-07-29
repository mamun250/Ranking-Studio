package com.rankingstudio.app.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class ImportRequest(
    val url: String
)

data class ImportResponse(
    val success: Boolean,
    val video: String?,
    val fileId: String?,
    val message: String?
)

// Data models for TikWM direct API scraper fallback
data class TikWMResponse(
    val code: Int?,
    val msg: String?,
    val data: TikWMData?
)

data class TikWMData(
    val id: String?,
    val title: String?,
    val play: String?,
    val wmplay: String?
)

interface TikTokApiService {
    @POST("import")
    suspend fun importTikTokVideo(
        @Body request: ImportRequest
    ): Response<ImportResponse>
}

/**
 * Helper object to resolve TikTok videos directly via TikWM API or custom base URL.
 */
object TikTokDirectResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Resolves TikTok video direct MP4 URL without watermark using TikWM public API.
     */
    suspend fun resolveViaTikWM(tiktokUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://www.tikwm.com/api/?url=${java.net.URLEncoder.encode(tiktokUrl, "UTF-8")}"
            val request = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful || bodyString.isBlank()) {
                return@withContext Result.failure(Exception("TikWM API returned HTTP ${response.code}"))
            }

            val tikwmResp = gson.fromJson(bodyString, TikWMResponse::class.java)

            if (tikwmResp.code == 0 && tikwmResp.data?.play != null) {
                var playUrl = tikwmResp.data.play
                if (playUrl.startsWith("/")) {
                    playUrl = "https://www.tikwm.com$playUrl"
                }
                Result.success(playUrl)
            } else {
                Result.failure(Exception(tikwmResp.msg ?: "TikTok video could not be resolved."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves TikTok video via custom backend host (e.g. https://my-server.com or http://192.168.1.50:3000).
     */
    suspend fun resolveViaCustomBackend(baseUrl: String, tiktokUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val endpointUrl = "${cleanBaseUrl}import"

            val jsonBody = gson.toJson(ImportRequest(tiktokUrl))
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(endpointUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful || bodyString.isBlank()) {
                return@withContext Result.failure(Exception("Custom backend returned HTTP ${response.code}"))
            }

            val importResp = gson.fromJson(bodyString, ImportResponse::class.java)
            if (importResp.success && !importResp.video.isNullOrBlank()) {
                Result.success(importResp.video)
            } else {
                Result.failure(Exception(importResp.message ?: "Custom backend failed to download video."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

