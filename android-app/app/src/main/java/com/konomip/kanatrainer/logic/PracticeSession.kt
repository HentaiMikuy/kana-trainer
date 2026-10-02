package com.konomip.kanatrainer.logic

import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.KanaItem
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class NextAction { BLOCKED, FINISH, ADVANCE }

/** 练习会话统计与流程判定，迁移自 practice-session.js。 */
object PracticeSession {

    fun calculateAccuracy(answered: Int, correct: Int): Int =
        if (answered == 0) 0 else Math.round(correct.toDouble() / answered * 100).toInt()

    /** 平均答题用时（秒），无记录时返回 0.0。 */
    fun getAverageAnswerTime(totalAnswerTimeMs: Long, timeAnsweredCount: Int): Double =
        if (timeAnsweredCount == 0) 0.0 else totalAnswerTimeMs / timeAnsweredCount / 1000.0

    fun formatSeconds(seconds: Double): String =
        String.format(Locale.US, "%.1fs", max(seconds, 0.0))

    fun getAverageAnswerTimeText(totalAnswerTimeMs: Long, timeAnsweredCount: Int): String =
        formatSeconds(getAverageAnswerTime(totalAnswerTimeMs, timeAnsweredCount))

    fun getCompletedQuestionCount(selectedAnswer: Boolean, currentIndex: Int): Int =
        if (selectedAnswer) currentIndex + 1 else currentIndex

    fun getProgressPercent(
        selectedAnswer: Boolean,
        currentIndex: Int,
        questionLimit: Int,
    ): Double {
        val limit = max(questionLimit, 0)
        if (limit == 0) return 0.0
        return getCompletedQuestionCount(selectedAnswer, currentIndex).toDouble() / limit * 100.0
    }

    fun getCompletionStats(answered: Int, correct: Int): Pair<Int, Int> {
        val accuracy = calculateAccuracy(answered, correct)
        val wrongCount = max(answered - correct, 0)
        return accuracy to wrongCount
    }

    /** 错题复习题量：题数 × 2，并限制在 [10, 50]；空范围返回 0。 */
    fun getMistakeReviewQuestionLimit(
        count: Int,
        minLimit: Int = 10,
        maxLimit: Int = 50,
        multiplier: Int = 2,
    ): Int {
        if (count <= 0) return 0
        return min(max(count * multiplier, minLimit), maxLimit)
    }

    /** 错题选择题候选池：完整题库在前、错题在后（后写覆盖），按假名 key 去重。 */
    fun createChoicePool(items: List<KanaItem>, allPracticeItems: List<KanaItem>): List<KanaItem> {
        val keyedItems = LinkedHashMap<String, KanaItem>()
        (allPracticeItems + items).forEach { item ->
            keyedItems[item.key] = item
        }
        return keyedItems.values.toList()
    }

    fun getNextQuestionAction(
        selectedAnswer: Boolean,
        currentIndex: Int,
        questionLimit: Int,
    ): NextAction {
        if (!selectedAnswer) return NextAction.BLOCKED
        return if (currentIndex + 1 >= questionLimit) NextAction.FINISH else NextAction.ADVANCE
    }
}
