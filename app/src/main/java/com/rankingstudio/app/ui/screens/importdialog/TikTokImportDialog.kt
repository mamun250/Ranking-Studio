package com.rankingstudio.app.ui.screens.importdialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rankingstudio.app.data.remote.ImportRequest
import com.rankingstudio.app.data.remote.TikTokApiService
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
    var isLoading by remember { mutableStateOf(false) }
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
                label = { Text("Paste TikTok URL") },
                placeholder = { Text("https://www.tiktok.com/@...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Terracotta,
                    unfocusedBorderColor = Color.Gray
                )
            )

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
                        text = "Downloading video from backend API...",
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
                        if (urlText.isBlank()) {
                            errorMessage = "Please enter a valid TikTok URL."
                            return@PapercraftButton
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val response = apiService.importTikTokVideo(ImportRequest(urlText.trim()))
                                isLoading = false
                                if (response.isSuccessful && response.body()?.success == true) {
                                    val videoUrl = response.body()?.video ?: ""
                                    onVideoDownloaded(videoUrl)
                                    onDismiss()
                                } else {
                                    errorMessage = response.body()?.message ?: "Failed to download video."
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.localizedMessage ?: "Error connecting to TikTok API server."
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
                    Text("Import to Timeline")
                }
            }
        }
    }
}
