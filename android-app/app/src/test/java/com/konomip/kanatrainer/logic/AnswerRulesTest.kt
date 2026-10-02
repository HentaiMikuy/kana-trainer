package com.konomip.kanatrainer.logic

import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.Script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerRulesTest {

    private fun questionFor(
        romaji: String,
        direction: Direction = Direction.KANA_TO_ROMAJI,
    ): Question = QuizEngine.makeQuestion(
        item = KanaData.getItemByRomaji(romaji)!!,
        direction = direction,
        mode = CharMode.KATAKANA,
    )

    @Test
    fun `accepts romaji variants and both kana scripts`() {
        val variantQuestion = questionFor("ji/di")
        assertEquals(listOf("ji", "di"), AnswerRules.getAcceptedAnswers(variantQuestion))
        assertTrue(AnswerRules.isAnswerCorrect(variantQuestion, " DI "))
        assertTrue(AnswerRules.isAnswerCorrect(variantQuestion, "di"))
        assertTrue(AnswerRules.isAnswerCorrect(variantQuestion, "JI"))

        val variantZu = questionFor("zu/du")
        assertTrue(AnswerRules.isAnswerCorrect(variantZu, "zu"))
        assertTrue(AnswerRules.isAnswerCorrect(variantZu, "du"))

        // 假名答案同时接受平假名与片假名
        val kanaQuestion = questionFor("shi", Direction.ROMAJI_TO_KANA)
        assertTrue(AnswerRules.isAnswerCorrect(kanaQuestion, "し"))
        assertTrue(AnswerRules.isAnswerCorrect(kanaQuestion, "シ"))
        assertFalse(AnswerRules.isAnswerCorrect(kanaQuestion, "ツ"))
    }

    @Test
    fun `normalizes whitespace and dashes in romaji answers`() {
        assertEquals("ji-di", AnswerRules.normalizeAnswerText("JI – DI"))
        assertEquals("", AnswerRules.normalizeAnswerText("  "))
        assertEquals("し", AnswerRules.normalizeKanaText(" し "))
    }

    @Test
    fun `question helpers expose labels and kana reading`() {
        val romajiQuestion = questionFor("shi")
        // 测试字符默认片假名模式，题干显示片假名
        assertEquals("シ", romajiQuestion.prompt)
        assertEquals("shi", romajiQuestion.answer)
        assertEquals(AnswerType.ROMAJI, romajiQuestion.answerType)
        assertEquals("选择对应的罗马音", romajiQuestion.helper)

        val kanaQuestion = questionFor("shi", Direction.ROMAJI_TO_KANA)
        assertEquals("shi", kanaQuestion.prompt)
        assertEquals("シ", kanaQuestion.answer)
        assertEquals(AnswerType.KANA, kanaQuestion.answerType)
        // makeQuestion 直接调用时默认 modeLabel 为「假名」；页面层会用设置覆盖
        assertEquals("选择对应的假名", kanaQuestion.helper)
        assertEquals("し", AnswerRules.questionKana(kanaQuestion.item, Script.HIRAGANA))
    }
}
