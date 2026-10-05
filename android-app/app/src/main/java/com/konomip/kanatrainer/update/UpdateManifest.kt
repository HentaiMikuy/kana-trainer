package com.konomip.kanatrainer.update

import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UpdateManifest(
    val schemaVersion: Int,
    val applicationId: String,
    val versionCode: Long,
    val versionName: String,
    val minSdk: Int,
    val apkUrl: String,
    val sizeBytes: Long,
    val sha256: String,
    val notes: String,
    val publishedAt: String,
)

enum class UpdateAvailability { CURRENT, AVAILABLE, UNSUPPORTED }

object UpdateRules {
    const val MAX_METADATA_BYTES = 128 * 1024
    const val MAX_APK_BYTES = 200L * 1024 * 1024
    val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String, repository: String, applicationId: String): UpdateManifest {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_METADATA_BYTES) { "更新信息过大" }
        val release = json.decodeFromString<UpdateManifest>(text)
        require(release.schemaVersion == 1) { "更新信息格式不受支持" }
        require(release.applicationId == applicationId) { "更新信息的应用标识不匹配" }
        require(release.versionCode in 1..2_100_000_000L) { "更新版本号无效" }
        require(release.versionName.isNotBlank() && release.versionName.length <= 80)
        require(release.minSdk in 1..1000)
        require(release.sizeBytes in 1..MAX_APK_BYTES) { "安装包大小无效" }
        require(Regex("[a-fA-F0-9]{64}").matches(release.sha256)) { "安装包校验信息无效" }
        require(release.notes.length <= 32_000 && release.publishedAt.length <= 80)
        val uri = URI(release.apkUrl)
        val prefix = "/$repository/releases/download/"
        require(uri.scheme == "https" && uri.host == "github.com" && uri.port == -1 &&
            uri.userInfo == null && uri.query == null && uri.fragment == null &&
            uri.rawPath.startsWith(prefix)) { "安装包地址不是本应用的 GitHub Release" }
        val parts = uri.rawPath.removePrefix(prefix).split('/')
        require(parts.size == 2 && parts.all { Regex("[A-Za-z0-9_.-]+").matches(it) && it != ".." } &&
            parts[1].endsWith(".apk")) { "安装包地址无效" }
        return release
    }

    fun availability(release: UpdateManifest, installedCode: Long, sdk: Int): UpdateAvailability = when {
        release.versionCode <= installedCode -> UpdateAvailability.CURRENT
        release.minSdk > sdk -> UpdateAvailability.UNSUPPORTED
        else -> UpdateAvailability.AVAILABLE
    }

    // Follow only GitHub's HTTPS asset redirects; never follow a redirect to HTTP.
    fun allowedTransportUrl(uri: URI): Boolean = uri.scheme == "https" &&
        uri.host in setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com") &&
        uri.userInfo == null && uri.port == -1
}
