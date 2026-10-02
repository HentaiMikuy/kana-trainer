package com.konomip.kanatrainer.logic

import com.konomip.kanatrainer.data.KanaItem
import com.konomip.kanatrainer.data.LearningRecord
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

/** 复习权重计算，迁移自 kana-core.js 的 calculateReviewWeight。 */
object ReviewWeight {

    const val RECOMMENDED_REVIEW_WEIGHT = 0.35
    private const val REVIEW_WEIGHT_MULTIPLIER = 0.18
    private const val REVIEW_STREAK_PENALTY = 0.12
    private const val REVIEW_REST_BONUS = 0.18
    private const val REVIEW_NEW_ITEM_BONUS = 0.08

    fun calculate(
        item: KanaItem,
        record: LearningRecord?,
        nowMs: Long = System.currentTimeMillis(),
    ): Double {
        if (record == null) {
            return 1.0 + REVIEW_NEW_ITEM_BONUS
        }

        val attempts = max(record.attempts, 1).toDouble()
        val misses = max(record.misses, 0)
        val masteredStreak = max(record.masteredStreak, 0)
        val missRate = misses / attempts
        val effectiveMisses = misses / (1 + masteredStreak * 0.45)
        val missesWeight =
            1.0 + min(effectiveMisses * REVIEW_WEIGHT_MULTIPLIER, 0.75) + missRate * 0.55
        val streakPenalty = min(masteredStreak * REVIEW_STREAK_PENALTY, 0.7)
        val lastPracticedTime = record.lastPracticedAt?.let { parsedEpochMs(it) }
        val restDays = if (lastPracticedTime == null) {
            0.0
        } else {
            max((nowMs - lastPracticedTime) / 86400000.0, 0.0)
        }
        val restBonus = min(restDays * REVIEW_REST_BONUS, 0.75)

        val weight = missesWeight + restBonus - streakPenalty
        return max(weight, RECOMMENDED_REVIEW_WEIGHT)
    }

    private fun parsedEpochMs(iso: String): Long? =
        try {
            Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            null
        }
}
