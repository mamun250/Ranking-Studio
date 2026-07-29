package com.rankingstudio.app.ui.screens.importdialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rankingstudio.app.data.remote.ImportRequest
import com.rankingstudio.app.data.remote.TikTokApiService
import com.rankingstudio.app.data.remote.TikTokDirectResolver
import com.rankingstudio.app.ui.components.PapercraftButton
import com.rankingstudio.app.ui.components.PapercraftCard
import com.rankingstudio.app.ui.theme.InkCharcoal
import com.rankingstudio.app.ui.theme.PaperWhite
import com.rankingstudio.app.ui.theme.Terracotta
import kotlinx.coroutines.launch

@Composable
fun TikTokImportDialog(
    apiService: TikTokApiService,
    onDismiss: () -> Unit,
    onVideoDownloaded: (String) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    var customServerUrl by remember { mutableStateOf("") }
    var showServerSettings by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Processing TikTok URL...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        PapercraftCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = PaperWhite
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "TikTok Import",
                    tint = Terracotta
                )
                Text(
                    text = "Import TikTok Video",
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkCharcoal
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("Paste TikTok Video URL") },
                placeholder = { Text("https://www.tiktok.com/@user/video/...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Terracotta,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showServerSettings = !showServerSettings }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showServerSettings) "Hide Custom Server Setting" else "Optional: Custom Server Host",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            AnimatedVisibility(visible = showServerSettings) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = customServerUrl,
                        onValueChange = { customServerUrl = it },
                        label = { Text("Server URL (e.g. http://192.168.1.50:3000)") },
                        placeholder = { Text("Leave blank to use direct download") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Terracotta,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Terracotta
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = InkCharcoal
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, enabled = !isLoading) {
                    Text("Cancel", color = InkCharcoal)
                }
                Spacer(modifier = Modifier.width(8.dp))
                PapercraftButton(
                    onClick = {
                        val inputUrl = urlText.trim()
                        if (inputUrl.isBlank() || !inputUrl.contains("tiktok.com")) {
                            errorMessage = "Please enter a valid TikTok video URL."
                            return@PapercraftButton
                        }
                        isLoading = true
                        errorMessage = null
                        statusMessage = "Resolving video download link..."

                        scope.launch {
                            try {
                                var resolvedUrl: String? = null

                                // 1. If user entered a custom backend URL, try that first
                                if (customServerUrl.isNotBlank()) {
                                    statusMessage = "Connecting to custom server..."
                                    val result = TikTokDirectResolver.resolveViaCustomBackend(customServerUrl.trim(), inputUrl)
                                    if (result.isSuccess) {
                                        resolvedUrl = result.getOrNull()
                                    }
                                }

                                // 2. Try direct TikWM public API scraper
                                if (resolvedUrl.isNullOrBlank()) {
                                    statusMessage = "Fetching direct HD video (No Watermark)..."
                                    val tikwmResult = TikTokDirectResolver.resolveViaTikWM(inputUrl)
                                    if (tikwmResult.isSuccess) {
                                        resolvedUrl = tikwmResult.getOrNull()
                                    }
                                }

                                // 3. Fallback to default Retrofit backend service if available
                                if (resolvedUrl.isNullOrBlank()) {
                                    statusMessage = "Trying local backend API fallback..."
                                    val response = apiService.importTikTokVideo(ImportRequest(inputUrl))
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        resolvedUrl = response.body()?.video
                                    }
                                }

                                isLoading = false
                                if (!resolvedUrl.isNullOrBlank()) {
                                    onVideoDownloaded(resolvedUrl)
                                    onDismiss()
                                } else {
                                    errorMessage = "Could not download TikTok video. Check URL or internet connection."
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.localizedMessage ?: "Error importing video."
                            }
                        }
                    },
                    enabled = !isLoading,
                    backgroundColor = Terracotta
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Video")
                }
            }
        }
    }
}

