package com.konomip.kanatrainer.update

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class UpdateClient(private val repository: String, private val applicationId: String) {
    suspend fun latest(): UpdateManifest? = withContext(Dispatchers.IO) {
        var uri = URI("https://github.com/$repository/releases/latest/download/update.json")
        repeat(6) {
            ensureActive()
            check(UpdateRules.allowedTransportUrl(uri)) { "更新服务器重定向地址无效" }
            val connection = uri.toURL().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 20_000
                connection.useCaches = false
                connection.setRequestProperty("User-Agent", "KanaTrainer-Android-Updater")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Cache-Control", "no-cache")
                when (connection.responseCode) {
                    404 -> return@withContext null
                    301, 302, 303, 307, 308 -> {
                        val location = connection.getHeaderField("Location") ?: throw IOException("更新地址缺失")
                        uri = uri.resolve(location)
                    }
                    200 -> {
                        if (connection.contentLengthLong > UpdateRules.MAX_METADATA_BYTES) {
                            throw IOException("更新信息过大")
                        }
                        val output = ByteArrayOutputStream()
                        connection.inputStream.use { input ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                if (output.size() + count > UpdateRules.MAX_METADATA_BYTES) {
                                    throw IOException("更新信息过大")
                                }
                                output.write(buffer, 0, count)
                            }
                        }
                        return@withContext UpdateRules.parse(output.toString("UTF-8"), repository, applicationId)
                    }
                    else -> throw IOException("检查更新失败（HTTP ${connection.responseCode}），请稍后重试")
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("更新服务器重定向次数过多")
    }
}
