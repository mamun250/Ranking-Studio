package com.rankingstudio.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class ImportRequest(
    val url: String
)

data class ImportResponse(
    val success: Boolean,
    val video: String?,
    val fileId: String?,
    val message: String?
)

interface TikTokApiService {
    @POST("import")
    suspend fun importTikTokVideo(
        @Body request: ImportRequest
    ): Response<ImportResponse>
}
