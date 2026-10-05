package com.konomip.kanatrainer.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.konomip.kanatrainer.BuildConfig
import com.konomip.kanatrainer.ui.edgeToEdgeRoot
import com.konomip.kanatrainer.update.UpdatePhase
import com.konomip.kanatrainer.update.UpdateViewModel
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(onBack: () -> Unit, viewModel: UpdateViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            clipData = ClipData.newRawUri("安装包", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {
            viewModel.showMessage("未找到系统安装程序，可使用浏览器下载安装")
        } catch (_: SecurityException) {
            viewModel.showMessage("系统阻止了安装，请检查“安装未知应用”权限")
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) {
            viewModel.verifyForInstall(::install)
        } else {
            viewModel.showMessage("尚未允许安装。开启“允许来自此来源的应用”后，再点击安装更新。")
        }
    }

    Column(Modifier.edgeToEdgeRoot().fillMaxSize()) {
        TopAppBar(title = { Text("应用更新", fontWeight = FontWeight.Bold) },
            navigationIcon = { TextButton(onClick = onBack) { Text("返回") } })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("当前版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
                    style = MaterialTheme.typography.titleMedium)
                Text("从 GitHub 获取正式版本，更新会保留学习记录。", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Button(onClick = viewModel::checkForUpdates, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()) { Text("检查更新") }
            }
            item {
                when (state.phase) {
                    UpdatePhase.RESTORING, UpdatePhase.CHECKING, UpdatePhase.VERIFYING -> Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(when (state.phase) {
                            UpdatePhase.RESTORING -> "正在恢复下载状态…"
                            UpdatePhase.CHECKING -> "正在检查更新…"
                            else -> "正在校验安装包…"
                        })
                    }
                    UpdatePhase.CURRENT -> if (state.message == null) Text("当前已是最新版本")
                    UpdatePhase.UNSUPPORTED -> Text("发现新版本，但当前 Android 系统版本不受支持。")
                    UpdatePhase.READY -> Text("下载完成，安装包已通过校验。")
                    else -> Unit
                }
                state.message?.let { Text(it, color = if (state.phase == UpdatePhase.FAILED)
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            state.release?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }?.let { release ->
                item {
                    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 1.dp) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("新版本 ${release.versionName}（${release.versionCode}）", fontWeight = FontWeight.Bold)
                            Text("安装包 ${megabytes(release.sizeBytes)} MB · ${release.publishedAt.take(10)}")
                            Text(release.notes.ifBlank { "此版本包含功能改进和问题修复。" })
                        }
                    }
                }
                item {
                    when (state.phase) {
                        UpdatePhase.AVAILABLE, UpdatePhase.FAILED -> {
                            Text("下载可能使用移动数据。", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = viewModel::download, modifier = Modifier.fillMaxWidth()) { Text("下载更新") }
                        }
                        UpdatePhase.DOWNLOADING -> {
                            LinearProgressIndicator(progress = { (state.downloadedBytes.toFloat() / release.sizeBytes).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth())
                            Text("${megabytes(state.downloadedBytes)} / ${megabytes(release.sizeBytes)} MB")
                            Text("可离开本页面，系统会继续下载。", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = viewModel::cancelDownload) { Text("取消下载") }
                        }
                        UpdatePhase.READY -> Button(onClick = {
                            viewModel.verifyForInstall { file ->
                                if (context.packageManager.canRequestPackageInstalls()) install(file)
                                else try {
                                    permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:${context.packageName}")))
                                } catch (_: ActivityNotFoundException) {
                                    viewModel.showMessage("请在系统设置中允许本应用安装未知应用，再返回安装。")
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("安装更新") }
                        else -> Unit
                    }
                }
            }
            item {
                OutlinedButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/${BuildConfig.UPDATE_REPOSITORY}/releases/latest")))
                    } catch (_: ActivityNotFoundException) { viewModel.showMessage("未找到浏览器") }
                }, modifier = Modifier.fillMaxWidth()) { Text("在浏览器查看发布页面") }
                Text("首次安装更新时，系统可能要求允许安装未知应用。", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun megabytes(bytes: Long): String = String.format(Locale.ROOT, "%.1f", bytes / (1024.0 * 1024))
