package com.konomip.kanatrainer.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 学习记录文件存储，等价于 Web 版 createStorageAdapter 的 localStorage 适配器。
 * 文件内容为学习记录 JSON 数组；读写失败时静默降级，练习仍可继续。
 */
class RecordStore(context: Context) {

    private val file: File = File(context.filesDir, RECORDS_FILE_NAME)
    private val mutex = Mutex()

    suspend fun load(): Map<String, LearningRecord> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext emptyMap()
            RecordJson.readRecords(file.readText())
        } catch (_: Exception) {
            emptyList()
        }.let { LearningRecords.recordsToMap(it) }
    }

    suspend fun save(records: Map<String, LearningRecord>): Boolean =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val temp = File(file.parentFile, file.name + ".tmp")
                    temp.writeText(RecordJson.writeRecords(records.values))
                    if (file.exists()) file.delete()
                    if (!temp.renameTo(file)) {
                        temp.copyTo(file, overwrite = true)
                        temp.delete()
                    }
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }

    suspend fun clear(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (file.exists()) file.delete() else true
            } catch (_: Exception) {
                false
            }
        }
    }

    companion object {
        const val RECORDS_FILE_NAME = "kana-trainer-learning-records-v1.json"
    }
}
