package com.konomip.kanatrainer.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object ApkVerifier {
    suspend fun verify(context: Context, file: File, release: UpdateManifest) = withContext(Dispatchers.IO) {
        verifyPayload(file, release)
        verifyPackage(context, file, release)
    }

    internal suspend fun verifyPayload(file: File, release: UpdateManifest) = withContext(Dispatchers.IO) {
        check(file.isFile && file.length() == release.sizeBytes) { "安装包不完整，请重新下载" }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        check(digest.digest().toHex().equals(release.sha256, ignoreCase = true)) {
            "安装包校验失败，请重新下载"
        }
    }

    private fun verifyPackage(context: Context, file: File, release: UpdateManifest) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
            else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        val manager = context.packageManager
        @Suppress("DEPRECATION")
        val archive = manager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: error("无法读取安装包，请重新下载")
        @Suppress("DEPRECATION")
        val installed = manager.getPackageInfo(context.packageName, flags)
        check(archive.packageName == context.packageName && archive.packageName == release.applicationId) {
            "安装包的应用标识不匹配"
        }
        check(PackageInfoCompat.getLongVersionCode(archive) == release.versionCode &&
            release.versionCode > PackageInfoCompat.getLongVersionCode(installed)) { "安装包版本不匹配或已经安装" }
        check(archive.applicationInfo?.minSdkVersion == release.minSdk && release.minSdk <= Build.VERSION.SDK_INT) {
            "安装包不支持当前 Android 版本"
        }
        check(sameSigners(signers(installed), signers(archive))) {
            "此安装包与当前应用签名不同。请先导出学习记录，手动安装正式版后再导入；正式版之间可直接更新。"
        }
    }

    internal fun sameSigners(installed: Set<String>, archive: Set<String>): Boolean =
        installed.isNotEmpty() && installed == archive

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return signatures?.map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).toHex() }?.toSet().orEmpty()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
