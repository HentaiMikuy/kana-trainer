package com.konomip.kanatrainer.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningRecordsTest {

    @Test
    fun `normalizes import payloads and drops unknown records`() {
        val payload = listOf(
            LearningRecord(
                romaji = "shi",
                hiragana = "し",
                katakana = "シ",
                group = "wrong",
                attempts = 1,
                correct = 2,
                misses = 1,
                masteredStreak = 9,
            ),
            LearningRecord(romaji = "missing", hiragana = "x", katakana = "x"),
        )
        val normalized = LearningRecords.normalizeLearningRecords(payload)

        assertEquals(1, normalized.size)
        assertEquals("s", normalized[0].group)
        assertEquals(3, normalized[0].attempts)
        assertEquals(2, normalized[0].masteredStreak)
    }

    @Test
    fun `keeps only currently weak records in the long term mistake pool`() {
        val weak = LearningRecord(
            romaji = "shi", hiragana = "し", katakana = "シ", group = "s",
            attempts = 3, correct = 1, misses = 2, masteredStreak = 0,
        )
        val recovered = LearningRecord(
            romaji = "ka", hiragana = "か", katakana = "カ", group = "k",
            attempts = 10, correct = 9, misses = 1, masteredStreak = 5,
        )
        val neverMissed = LearningRecord(
            romaji = "a", hiragana = "あ", katakana = "ア", group = "a",
            attempts = 5, correct = 5, misses = 0, masteredStreak = 5,
        )

        assertTrue(LearningRecords.isLongTermWeakRecord(weak))
        assertFalse(LearningRecords.isLongTermWeakRecord(recovered))
        assertFalse(LearningRecords.isLongTermWeakRecord(neverMissed))

        val mistakes = LearningRecords.getLongTermMistakeRecords(
            mapOf("w" to weak, "r" to recovered, "n" to neverMissed),
        )
        assertEquals(setOf("w"), mistakes.keys)
    }

    @Test
    fun `merges imported records over existing records`() {
        val item = KanaData.getItemByRomaji("shi")!!
        val existing = LearningRecords.normalizeRecord(
            LearningRecords.createRecord(item).copy(attempts = 1, correct = 1),
        )
        val current = mapOf(item.key to existing)
        val result = LearningRecords.mergeLearningRecords(
            current,
            listOf(item.copy().let { LearningRecord(it.romaji, it.hiragana, it.katakana, it.group, attempts = 5, correct = 2, misses = 3) }),
        )

        assertEquals(1, result.importedCount)
        assertEquals(1, result.records.size)
        assertEquals(5, result.records.getValue(item.key).attempts)
    }

    @Test
    fun `applies practice results without mutating the input map`() {
        val item = KanaData.getItemByRomaji("a")!!
        val source = emptyMap<String, LearningRecord>()
        val result = LearningRecords.applyPracticeResult(
            source,
            item,
            isCorrect = false,
            now = Instant.parse("2026-06-03T00:00:00.000Z"),
        )
        val record = result.records.getValue(item.key)

        assertEquals(0, source.size)
        assertEquals(1, record.attempts)
        assertEquals(0, record.correct)
        assertEquals(1, record.misses)
        assertEquals(0, record.masteredStreak)
        assertEquals("2026-06-03T00:00:00.000Z", record.lastPracticedAt)

        // 答对时连对次数累计，答错时归零
        val correctResult = LearningRecords.applyPracticeResult(
            result.records, item, isCorrect = true,
        )
        assertEquals(1, correctResult.record.masteredStreak)
        val wrongAgain = LearningRecords.applyPracticeResult(
            correctResult.records, item, isCorrect = false,
        )
        assertEquals(0, wrongAgain.record.masteredStreak)
    }

    @Test
    fun `builds export payloads and filenames with stable timestamps`() {
        val item = KanaData.getItemByRomaji("a")!!
        val map = mapOf(item.key to LearningRecords.createRecord(item))
        val now = Instant.parse("2026-06-03T01:02:03.004Z")
        val payload = LearningRecords.buildLearningExportData(map.values, now)

        assertEquals(1, payload.schemaVersion)
        assertEquals("2026-06-03T01:02:03.004Z", payload.exportedAt)
        assertEquals(1, payload.records.size)
        assertEquals(
            "kana-learning-records-2026-06-03T01-02-03-004Z.json",
            LearningRecords.getLearningExportFilename(now),
        )
    }

    @Test
    fun `record accuracy rounds to whole percent`() {
        assertEquals(0, LearningRecords.getRecordAccuracy(LearningRecord("a", "あ", "ア", "a")))
        assertEquals(75, LearningRecords.getRecordAccuracy(
            LearningRecord("a", "あ", "ア", "a", attempts = 4, correct = 3),
        ))
        assertEquals(100, LearningRecords.getRecordAccuracy(
            LearningRecord("a", "あ", "ア", "a", attempts = 5, correct = 5),
        ))
    }
}
