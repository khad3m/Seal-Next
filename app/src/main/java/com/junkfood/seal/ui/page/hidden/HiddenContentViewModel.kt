package com.junkfood.seal.ui.page.hidden

import android.media.MediaScannerConnection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junkfood.seal.App
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.FileUtil.getFileSize
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val VAULT_AUDIO_EXT = setOf("m4a", "mp3", "opus", "ogg", "flac", "wav", "aac")

class HiddenContentViewModel : ViewModel() {

    val hiddenVideoListFlow = DatabaseUtil.getHiddenDownloadHistoryFlow()

    val fileSizeMapFlow =
        hiddenVideoListFlow.map { list ->
            list.associate { it.id to it.videoPath.getFileSize() }
        }.flowOn(Dispatchers.IO)

    private val _vaultFiles = MutableStateFlow<List<File>>(emptyList())
    val vaultFilesFlow = _vaultFiles.asStateFlow()

    init {
        refreshVaultFiles()
    }

    fun refreshVaultFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val dirs =
                listOf(App.videoDownloadDir, App.audioDownloadDir)
                    .distinct()
                    .map { File(it, ".seal_vault") }
                    .filter { it.exists() && it.isDirectory }
            val found =
                dirs
                    .flatMap { it.listFiles()?.toList().orEmpty() }
                    .filter { it.isFile }
                    .distinctBy { it.absolutePath }
                    .sortedByDescending { it.lastModified() }
            _vaultFiles.value = found
        }
    }

    fun unhideItem(info: DownloadedVideoInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseUtil.unhideItem(info)
        }
    }

    fun restoreVaultFile(file: File, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetDir =
                File(
                    if (file.extension.lowercase() in VAULT_AUDIO_EXT) App.audioDownloadDir
                    else App.videoDownloadDir
                ).apply { mkdirs() }
            val dest =
                File(targetDir, file.name).let { candidate ->
                    if (!candidate.exists()) candidate
                    else File(targetDir, "${System.currentTimeMillis()}_${file.name}")
                }
            val moved =
                runCatching {
                    if (file.renameTo(dest)) true
                    else {
                        file.copyTo(dest, overwrite = true)
                        file.delete()
                    }
                }.getOrDefault(false)
            if (moved) {
                MediaScannerConnection.scanFile(
                    App.context,
                    arrayOf(dest.absolutePath),
                    null,
                    null,
                )
            }
            refreshVaultFiles()
            kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(moved) }
        }
    }

    fun deleteVaultFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { file.delete() }
            refreshVaultFiles()
        }
    }
}
