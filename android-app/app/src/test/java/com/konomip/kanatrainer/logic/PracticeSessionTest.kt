package com.konomip.kanatrainer.logic

import com.konomip.kanatrainer.data.KanaData
import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeSessionTest {

    @Test
    fun `accuracy handles empty and rounding cases`() {
        assertEquals(0, PracticeSession.calculateAccuracy(0, 0))
        assertEquals(75, PracticeSession.calculateAccuracy(4, 3))
        assertEquals(100, PracticeSession.calculateAccuracy(10, 10))
    }

    @Test
    fun `average answer time is zero without timed answers`() {
        assertEquals(0.0, PracticeSession.getAverageAnswerTime(0, 0), 1e-9)
        assertEquals("0.0s", PracticeSession.getAverageAnswerTimeText(0, 0))
        assertEquals(0.75, PracticeSession.getAverageAnswerTime(1500, 2), 1e-9)
        assertEquals("0.8s", PracticeSession.getAverageAnswerTimeText(1500, 2))
    }

    @Test
    fun `progress uses completed questions over the limit`() {
        assertEquals(0.0, PracticeSession.getProgressPercent(false, 0, 20), 1e-9)
        assertEquals(10.0, PracticeSession.getProgressPercent(false, 2, 20), 1e-9)
        assertEquals(15.0, PracticeSession.getProgressPercent(true, 2, 20), 1e-9)
        assertEquals(0.0, PracticeSession.getProgressPercent(true, 5, 0), 1e-9)
        assertEquals(3, PracticeSession.getCompletedQuestionCount(true, 2))
        assertEquals(2, PracticeSession.getCompletedQuestionCount(false, 2))
    }

    @Test
    fun `mistake review limit clamps to the configured bounds`() {
        assertEquals(0, PracticeSession.getMistakeReviewQuestionLimit(0))
        assertEquals(10, PracticeSession.getMistakeReviewQuestionLimit(3))
        assertEquals(20, PracticeSession.getMistakeReviewQuestionLimit(10))
        assertEquals(50, PracticeSession.getMistakeReviewQuestionLimit(30))
        assertEquals(50, PracticeSession.getMistakeReviewQuestionLimit(500))
    }

    @Test
    fun `choice pool lets mistake items override the full bank`() {
        val all = KanaData.getAllPracticeItems()
        val mistakeItem = KanaData.getItemByRomaji("shi")!!.copy(group = "wrong-group")
        val pool = PracticeSession.createChoicePool(listOf(mistakeItem), all)

        assertEquals(all.size, pool.size)
        assertEquals("wrong-group", pool.first { it.key == mistakeItem.key }.group)
    }

    @Test
    fun `next question action follows selection and index`() {
        assertEquals(NextAction.BLOCKED, PracticeSession.getNextQuestionAction(false, 0, 20))
        assertEquals(NextAction.ADVANCE, PracticeSession.getNextQuestionAction(true, 0, 20))
        assertEquals(NextAction.ADVANCE, PracticeSession.getNextQuestionAction(true, 18, 20))
        assertEquals(NextAction.FINISH, PracticeSession.getNextQuestionAction(true, 19, 20))
    }

    @Test
    fun `completion stats expose accuracy and wrong count`() {
        assertEquals(75 to 1, PracticeSession.getCompletionStats(4, 3))
        assertEquals(0 to 0, PracticeSession.getCompletionStats(0, 0))
    }
}
