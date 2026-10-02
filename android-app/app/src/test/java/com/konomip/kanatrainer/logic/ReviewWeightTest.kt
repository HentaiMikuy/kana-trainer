package com.konomip.kanatrainer.logic

import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.LearningRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewWeightTest {

    private val item = KanaData.getItemByRomaji("shi")!!
    private val nowMs = Instant_epochMs("2026-09-13T00:00:00.000Z")

    private fun Instant_epochMs(iso: String): Long =
        java.time.Instant.parse(iso).toEpochMilli()

    @Test
    fun `new items get the small bonus weight`() {
        assertEquals(1.08, ReviewWeight.calculate(item, null), 1e-9)
        assertEquals(1.08, ReviewWeight.calculate(item, null, nowMs), 1e-9)
    }

    @Test
    fun `weights weak records above mastered records`() {
        val weak = LearningRecord(
            romaji = "shi", hiragana = "し", katakana = "シ", group = "s",
            attempts = 5, correct = 2, misses = 3, masteredStreak = 0,
            lastPracticedAt = "2026-09-12T00:00:00.000Z",
        )
        val mastered = LearningRecord(
            romaji = "shi", hiragana = "し", katakana = "シ", group = "s",
            attempts = 20, correct = 20, misses = 0, masteredStreak = 10,
            lastPracticedAt = "2026-09-12T00:00:00.000Z",
        )

        val weakWeight = ReviewWeight.calculate(item, weak, nowMs)
        val masteredWeight = ReviewWeight.calculate(item, mastered, nowMs)
        assertTrue(weakWeight > masteredWeight)
    }

    @Test
    fun `weights never drop below the recommended floor`() {
        val mastered = LearningRecord(
            romaji = "shi", hiragana = "し", katakana = "シ", group = "s",
            attempts = 100, correct = 100, misses = 0, masteredStreak = 50,
            lastPracticedAt = "2026-09-13T00:00:00.000Z",
        )
        val weight = ReviewWeight.calculate(item, mastered, nowMs)
        assertEquals(ReviewWeight.RECOMMENDED_REVIEW_WEIGHT, weight, 1e-9)
    }

    @Test
    fun `rest bonus grows with days since last practice`() {
        val record = LearningRecord(
            romaji = "shi", hiragana = "し", katakana = "シ", group = "s",
            attempts = 4, correct = 3, misses = 1, masteredStreak = 1,
            lastPracticedAt = "2026-09-12T00:00:00.000Z",
        )
        val fresh = ReviewWeight.calculate(item, record, nowMs)
        val stale = ReviewWeight.calculate(item, record, nowMs + 30L * 86400000L)
        assertTrue(stale > fresh)
    }

    @Test
    fun `invalid timestamps are treated as never practiced`() {
        val record = LearningRecord(
            romaji = "shi", hiragana = "し", katakana = "シ", group = "s",
            attempts = 4, correct = 3, misses = 1, masteredStreak = 1,
            lastPracticedAt = "not-a-date",
        )
        val base = LearningRecord(
            romaji = "shi", hiragana = "し", katakana = "シ", group = "s",
            attempts = 4, correct = 3, misses = 1, masteredStreak = 1,
        )
        assertEquals(
            ReviewWeight.calculate(item, base, nowMs),
            ReviewWeight.calculate(item, record, nowMs),
            1e-9,
        )
    }
}
