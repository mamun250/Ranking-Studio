package com.rankingstudio.app.ui.screens.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rankingstudio.app.domain.model.RankingProject
import com.rankingstudio.app.exporter.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor() : ViewModel() {

    private val _options = MutableStateFlow(ExportOptions())
    val options: StateFlow<ExportOptions> = _options.asStateFlow()

    private val _progress = MutableStateFlow(ExportProgress())
    val progress: StateFlow<ExportProgress> = _progress.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportSuccess = MutableStateFlow(false)
    val exportSuccess: StateFlow<Boolean> = _exportSuccess.asStateFlow()

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateResolution(resolution: ExportResolution) {
        _options.value = _options.value.copy(resolution = resolution)
    }

    fun updateFrameRate(frameRate: ExportFrameRate) {
        _options.value = _options.value.copy(frameRate = frameRate)
    }

    fun updateBitrate(bitrate: ExportBitrate) {
        _options.value = _options.value.copy(bitrate = bitrate)
    }

    fun startExport(context: Context, project: RankingProject) {
        if (_isExporting.value) return

        _isExporting.value = true
        _exportSuccess.value = false
        _errorMessage.value = null
        _progress.value = ExportProgress(statusText = "Preparing frame-accurate render...")

        viewModelScope.launch(Dispatchers.IO) {
            val outputFile = VideoExporter.getExportOutputFile(context)
            _exportedFile.value = outputFile

            val success = VideoExporter.exportProjectVideoFrameAccurate(
                context = context,
                project = project,
                options = _options.value,
                outputFile = outputFile,
                onProgress = { progressData ->
                    _progress.value = progressData
                }
            )

            _isExporting.value = false
            if (success) {
                _exportSuccess.value = true
            } else {
                _errorMessage.value = "Export failed. Please check source videos and try again."
            }
        }
    }

    fun openExportedVideo(context: Context) {
        val file = _exportedFile.value ?: return
        if (!file.exists()) return

        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open Video"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareExportedVideo(context: Context) {
        val file = _exportedFile.value ?: return
        if (!file.exists()) return

        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Video"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
