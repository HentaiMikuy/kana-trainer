package com.konomip.kanatrainer.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToJsonElement

/**
 * 学习记录 JSON 编解码。
 *
 * 存储文件与 Web 版 localStorage 一样保存裸数组；导出/导入 payload 与
 * Web 版导出文件一致（`schemaVersion` + `exportedAt` + `records`，
 * 同时兼容裸数组导入）。
 */
object RecordJson {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** 解析导入内容：接受裸数组或 {records:[...]}，无效记录由规范化阶段剔除。 */
    fun parseRecordsPayload(text: String): List<LearningRecord> {
        val element: JsonElement = json.parseToJsonElement(text)
        return when (element) {
            is JsonArray -> json.decodeFromJsonElement<List<LearningRecord>>(element)
            is JsonObject -> {
                val payload = json.decodeFromJsonElement<ExportPayload>(element)
                payload.records
            }
            else -> emptyList()
        }
    }

    /** 存储文件序列化：与 Web 版 localStorage 一致保存裸数组。 */
    fun writeRecords(records: Collection<LearningRecord>): String =
        json.encodeToString(records.toList())

    fun readRecords(text: String): List<LearningRecord> =
        json.decodeFromJsonElement<List<LearningRecord>>(json.parseToJsonElement(text))

    fun writeExportPayload(payload: ExportPayload): String =
        json.encodeToString(ExportPayload.serializer(), payload) + "\n"
}
