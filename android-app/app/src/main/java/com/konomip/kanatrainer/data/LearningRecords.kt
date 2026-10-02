package com.konomip.kanatrainer.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

/**
 * 单条学习记录，字段与 Web 版 localStorage 记录一致，
 * JSON 导出/导入格式与 Web 版互通。
 */
@Serializable
data class LearningRecord(
    val romaji: String,
    val hiragana: String,
    val katakana: String,
    val group: String = "base",
    val attempts: Int = 0,
    val correct: Int = 0,
    val misses: Int = 0,
    val masteredStreak: Int = 0,
    @SerialName("lastPracticedAt") val lastPracticedAt: String? = null,
)

/** 导出 payload，与 Web 版 buildLearningExportData 输出结构一致。 */
@Serializable
data class ExportPayload(
    @SerialName("schemaVersion") val schemaVersion: Int = LearningRecords.EXPORT_SCHEMA_VERSION,
    @SerialName("exportedAt") val exportedAt: String,
    @SerialName("records") val records: List<LearningRecord> = emptyList(),
)

data class MergeResult(
    val records: Map<String, LearningRecord>,
    val importedCount: Int,
    val importedRecords: List<LearningRecord>,
)

data class PracticeResult(
    val records: Map<String, LearningRecord>,
    val record: LearningRecord,
    val key: String,
)

/** 学习记录纯逻辑：规范化、长期薄弱项、导入合并、练习结果写入、导出 payload。 */
object LearningRecords {

    const val EXPORT_SCHEMA_VERSION = 1

    /** 与 Web 版 toISOString 相同的毫秒精度 UTC 格式。 */
    private val ISO_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

    fun toIsoTimestamp(now: Instant): String = ISO_FORMAT.format(now)

    fun createRecord(item: KanaItem): LearningRecord = LearningRecord(
        romaji = item.romaji,
        hiragana = item.hiragana,
        katakana = item.katakana,
        group = item.group,
    )

    /** 与 Web 版 normalizeRecord 一致：以题库为准回填字段并钳制计数值。 */
    fun normalizeRecord(record: LearningRecord, canonicalItem: KanaItem? = null): LearningRecord {
        val item = canonicalItem ?: KanaItem(
            romaji = record.romaji,
            hiragana = record.hiragana,
            katakana = record.katakana,
            group = record.group,
        )
        val rawAttempts = max(record.attempts, 0)
        val rawCorrect = max(record.correct, 0)
        val rawMisses = max(record.misses, 0)
        val attempts = max(rawAttempts, rawCorrect + rawMisses)
        val correct = min(rawCorrect, attempts)
        val misses = min(rawMisses, max(attempts - correct, 0))
        val masteredStreak = min(max(record.masteredStreak, 0), correct)
        return LearningRecord(
            romaji = item.romaji,
            hiragana = item.hiragana,
            katakana = item.katakana,
            group = item.group,
            attempts = attempts,
            correct = correct,
            misses = misses,
            masteredStreak = masteredStreak,
            lastPracticedAt = record.lastPracticedAt,
        )
    }

    fun getRecordAccuracy(record: LearningRecord): Int {
        val attempts = max(record.attempts, 0)
        val correct = max(record.correct, 0)
        return if (attempts == 0) 0 else Math.round(correct.toDouble() / attempts * 100).toInt()
    }

    fun isLongTermWeakRecord(record: LearningRecord): Boolean {
        val misses = max(record.misses, 0)
        if (misses == 0) return false
        val accuracy = getRecordAccuracy(record)
        val masteredStreak = max(record.masteredStreak, 0)
        return masteredStreak < 3 || accuracy < 75
    }

    /** 规范化导入记录：只保留题库中存在的假名。 */
    fun normalizeLearningRecords(source: List<LearningRecord>): List<LearningRecord> =
        source.mapNotNull { record ->
            val canonicalItem = KanaData.getCanonicalPracticeItem(record) ?: return@mapNotNull null
            normalizeRecord(record, canonicalItem)
        }

    fun recordsToMap(records: List<LearningRecord>): Map<String, LearningRecord> =
        normalizeLearningRecords(records).associateBy { keyOf(it) }

    fun getLongTermMistakeRecords(
        records: Map<String, LearningRecord>,
    ): Map<String, LearningRecord> =
        records.filterValues { isLongTermWeakRecord(it) }

    fun buildLearningExportData(
        records: Collection<LearningRecord>,
        now: Instant = Instant.now(),
    ): ExportPayload = ExportPayload(
        schemaVersion = EXPORT_SCHEMA_VERSION,
        exportedAt = toIsoTimestamp(now),
        records = records.toList(),
    )

    fun getLearningExportFilename(now: Instant = Instant.now()): String {
        val stamp = toIsoTimestamp(now).replace(":", "-").replace(".", "-")
        return "kana-learning-records-$stamp.json"
    }

    fun mergeLearningRecords(
        currentRecords: Map<String, LearningRecord>,
        payload: List<LearningRecord>,
    ): MergeResult {
        val importedRecords = normalizeLearningRecords(payload)
        val records = currentRecords.toMutableMap()
        importedRecords.forEach { record ->
            records[keyOf(record)] = record
        }
        return MergeResult(
            records = records,
            importedCount = importedRecords.size,
            importedRecords = importedRecords,
        )
    }

    fun applyPracticeResult(
        records: Map<String, LearningRecord>,
        item: KanaItem,
        isCorrect: Boolean,
        now: Instant = Instant.now(),
    ): PracticeResult {
        val key = item.key
        val existing = records[key] ?: createRecord(item)
        val record = normalizeRecord(existing)
        val next = record.copy(
            attempts = record.attempts + 1,
            lastPracticedAt = toIsoTimestamp(now),
            correct = record.correct + if (isCorrect) 1 else 0,
            misses = record.misses + if (isCorrect) 0 else 1,
            masteredStreak = if (isCorrect) record.masteredStreak + 1 else 0,
        )
        val nextRecords = records.toMutableMap()
        nextRecords[key] = next
        return PracticeResult(records = nextRecords, record = next, key = key)
    }

    private fun keyOf(record: LearningRecord): String =
        "${record.hiragana}|${record.katakana}|${record.romaji}"
}
