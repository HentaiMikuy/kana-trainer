package com.konomip.kanatrainer.update

import android.app.Application
import android.app.DownloadManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.konomip.kanatrainer.BuildConfig
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

enum class UpdatePhase { RESTORING, IDLE, CHECKING, CURRENT, AVAILABLE, UNSUPPORTED, DOWNLOADING, VERIFYING, READY, FAILED }

data class UpdateUiState(
    val phase: UpdatePhase = UpdatePhase.RESTORING,
    val release: UpdateManifest? = null,
    val downloadedBytes: Long = 0,
    val message: String? = null,
) {
    val busy: Boolean get() = phase in setOf(UpdatePhase.RESTORING, UpdatePhase.CHECKING,
        UpdatePhase.DOWNLOADING, UpdatePhase.VERIFYING)
}

/** DownloadManager owns the transfer. This model restores its id after process death. */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val manager = app.getSystemService(DownloadManager::class.java)
    private val preferences = app.getSharedPreferences("app-update", 0)
    private val client = UpdateClient(BuildConfig.UPDATE_REPOSITORY, app.packageName)
    private val mutableState = MutableStateFlow(UpdateUiState())
    val state = mutableState.asStateFlow()
    private var task: Job? = null
    private var downloadId = -1L

    init {
        runTask {
            downloadId = preferences.getLong("downloadId", -1L)
            val text = preferences.getString("release", null)
            if (downloadId >= 0 && text != null) {
                val release = UpdateRules.parse(text, BuildConfig.UPDATE_REPOSITORY, app.packageName)
                if (UpdateRules.availability(release, BuildConfig.VERSION_CODE.toLong(), Build.VERSION.SDK_INT) ==
                    UpdateAvailability.AVAILABLE) {
                    mutableState.value = UpdateUiState(UpdatePhase.DOWNLOADING, release)
                    watchDownload(release)
                    return@runTask
                }
            }
            clearDownload()
            mutableState.value = UpdateUiState(UpdatePhase.IDLE)
        }
    }

    fun checkForUpdates() {
        if (mutableState.value.busy) return
        runTask {
            mutableState.value = UpdateUiState(UpdatePhase.CHECKING)
            clearDownload()
            val release = client.latest()
            val phase = when {
                release == null -> UpdatePhase.CURRENT
                else -> when (UpdateRules.availability(release, BuildConfig.VERSION_CODE.toLong(), Build.VERSION.SDK_INT)) {
                    UpdateAvailability.CURRENT -> UpdatePhase.CURRENT
                    UpdateAvailability.AVAILABLE -> UpdatePhase.AVAILABLE
                    UpdateAvailability.UNSUPPORTED -> UpdatePhase.UNSUPPORTED
                }
            }
            mutableState.value = UpdateUiState(phase, release,
                message = if (release == null) "尚未发布可用的更新，请稍后再检查。" else null)
        }
    }

    fun download() {
        val state = mutableState.value
        val release = state.release ?: return
        if (state.busy || UpdateRules.availability(release, BuildConfig.VERSION_CODE.toLong(), Build.VERSION.SDK_INT) !=
            UpdateAvailability.AVAILABLE) return
        runTask {
            mutableState.value = UpdateUiState(UpdatePhase.DOWNLOADING, release)
            clearDownload()
            withContext(Dispatchers.IO) {
                val file = apkFile(release)
                check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs()) { "无法创建下载目录" }
                check(!file.exists() || file.delete()) { "无法清理旧安装包" }
                val request = DownloadManager.Request(Uri.parse(release.apkUrl))
                    .setTitle("五十音记忆测试 ${release.versionName}")
                    .setDescription("下载应用更新")
                    .setMimeType("application/vnd.android.package-archive")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)
                    .setDestinationUri(Uri.fromFile(file))
                downloadId = manager.enqueue(request)
                check(preferences.edit().putLong("downloadId", downloadId)
                    .putString("release", UpdateRules.json.encodeToString(release)).commit()) { "无法保存下载状态" }
            }
            watchDownload(release)
        }
    }

    fun cancelDownload() {
        val release = mutableState.value.release
        runTask {
            mutableState.update { it.copy(phase = UpdatePhase.RESTORING) }
            clearDownload()
            mutableState.value = UpdateUiState(if (release == null) UpdatePhase.IDLE else UpdatePhase.AVAILABLE,
                release, message = "下载已取消")
        }
    }

    fun verifyForInstall(onVerified: (File) -> Unit) {
        val state = mutableState.value
        val release = state.release ?: return
        if (state.phase != UpdatePhase.READY) return
        runTask {
            mutableState.update { it.copy(phase = UpdatePhase.VERIFYING, message = null) }
            val file = apkFile(release)
            ApkVerifier.verify(app, file, release)
            mutableState.update { it.copy(phase = UpdatePhase.READY) }
            onVerified(file)
        }
    }

    fun showMessage(message: String) { mutableState.update { it.copy(message = message) } }

    private suspend fun watchDownload(release: UpdateManifest) {
        while (true) {
            val progress = withContext(Dispatchers.IO) {
                manager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
                    check(cursor != null && cursor.moveToFirst()) { "下载任务已被移除，请重新下载" }
                    Triple(cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)))
                }
            }
            check(progress.second <= release.sizeBytes) { "下载大小与更新信息不匹配" }
            when (progress.first) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    mutableState.update { it.copy(phase = UpdatePhase.VERIFYING, message = null) }
                    ApkVerifier.verify(app, apkFile(release), release)
                    mutableState.update { it.copy(phase = UpdatePhase.READY, downloadedBytes = release.sizeBytes) }
                    return
                }
                DownloadManager.STATUS_FAILED -> error(when (progress.third) {
                    DownloadManager.ERROR_INSUFFICIENT_SPACE -> "存储空间不足，请清理后重新下载"
                    else -> "下载失败，请检查网络后重试（${progress.third}）"
                })
                else -> mutableState.update { it.copy(downloadedBytes = progress.second.coerceAtLeast(0),
                    message = if (progress.first == DownloadManager.STATUS_PAUSED) "下载已暂停，等待网络或系统重试…" else null) }
            }
            delay(500)
        }
    }

    private fun apkFile(release: UpdateManifest): File {
        val directory = app.getExternalFilesDir(null) ?: error("无法访问应用下载目录")
        return File(directory, "updates/kana-trainer-${release.versionCode}.apk")
    }

    private suspend fun clearDownload() = withContext(Dispatchers.IO) {
        if (downloadId >= 0) manager.remove(downloadId)
        downloadId = -1
        check(preferences.edit().clear().commit()) { "无法清理下载状态" }
        app.getExternalFilesDir(null)?.let { directory ->
            File(directory, "updates").listFiles()?.filter { it.isFile && it.name.endsWith(".apk") }?.forEach { it.delete() }
        }
    }

    private fun runTask(block: suspend () -> Unit) {
        val previous = task
        previous?.cancel()
        task = viewModelScope.launch {
            // Await in-flight DownloadManager / preference writes before a new operation.
            previous?.join()
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled // Retain the system download when this ViewModel is cleared.
            } catch (error: Exception) {
                try { clearDownload() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { }
                val message = when (error) {
                    is UnknownHostException -> "无法连接 GitHub，请检查网络后重试"
                    is SocketTimeoutException -> "连接 GitHub 超时，请稍后重试或使用浏览器下载"
                    is kotlinx.serialization.SerializationException -> "更新信息无法读取，请稍后重试"
                    is IOException -> "网络或文件读取失败，请检查网络与存储后重试"
                    is SecurityException -> "系统未允许此操作，请检查下载或安装权限"
                    else -> error.message ?: "更新失败，请稍后重试"
                }
                mutableState.update { it.copy(phase = UpdatePhase.FAILED, message = message) }
            }
        }
    }
}
