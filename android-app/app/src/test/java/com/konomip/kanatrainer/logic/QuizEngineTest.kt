package com.konomip.kanatrainer.logic

import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.Script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizEngineTest {

    /** 恒定返回 0 的随机源，对应 Web 版测试中的 `random: () => 0`。 */
    private object ZeroRandom : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }

    private fun identityShuffle(items: List<String>, random: Random): List<String> = items

    @Test
    fun `builds a practice pool from selected rows and enabled extras`() {
        val pool = QuizEngine.getPracticePool(
            selectedRows = setOf("k"),
            includeDakuten = true,
            includeSmallKana = true,
        )

        assertEquals(16, pool.size)
        assertEquals(
            listOf(
                "ka", "ki", "ku", "ke", "ko",
                "ga", "gi", "gu", "ge", "go",
                "kya", "kyu", "kyo", "gya", "gyu", "gyo",
            ),
            pool.map { it.romaji },
        )
        // 浊音 / 拗音条目继承行字母分组，与 Web 版 toItem 行为一致
        assertEquals("k", pool[5].group)
        assertEquals("k", pool[10].group)
    }

    @Test
    fun `makes questions with forced scripts and session labels`() {
        val item = KanaData.withQuestionScript(KanaData.getItemByRomaji("nu")!!, Script.HIRAGANA)
        val question = QuizEngine.makeQuestion(
            item = item,
            direction = Direction.ROMAJI_TO_KANA,
            mode = CharMode.KATAKANA,
            activeSessionType = SessionType.CONFUSING,
            modeLabel = "假名",
        )

        assertEquals("nu", question.prompt)
        assertEquals("ぬ", question.answer)
        assertEquals(AnswerType.KANA, question.answerType)
        assertEquals(Script.HIRAGANA, question.answerScript)
        assertEquals("易混淆专项：选择对应的假名", question.helper)
    }

    @Test
    fun `mistake sessions label questions as mistake review`() {
        val question = QuizEngine.makeQuestion(
            item = KanaData.getItemByRomaji("ka")!!,
            activeSessionType = SessionType.MISTAKES,
        )
        assertEquals("错题复习：选择对应的罗马音", question.helper)
    }

    @Test
    fun `prioritizes confusing distractors and removes duplicate options`() {
        val question = QuizEngine.makeQuestion(
            item = KanaData.getItemByRomaji("shi")!!,
            direction = Direction.ROMAJI_TO_KANA,
            mode = CharMode.KATAKANA,
        )
        val options = QuizEngine.makeOptions(
            question = question,
            optionPool = KanaData.getAllPracticeItems(),
            priorityDistractors = listOf(
                KanaData.withQuestionScript(KanaData.getItemByRomaji("tsu")!!, Script.KATAKANA),
            ),
            random = ZeroRandom,
            shuffle = ::identityShuffle,
        )

        assertEquals(8, options.size)
        assertEquals(options.size, options.toSet().size)
        assertEquals("シ", options[0])
        assertEquals("ツ", options[1])
    }

    @Test
    fun `random options always contain the answer without duplicates`() {
        repeat(20) { seed ->
            val question = QuizEngine.makeQuestion(
                item = KanaData.getItemByRomaji("shi")!!,
                direction = Direction.ROMAJI_TO_KANA,
                mode = CharMode.KATAKANA,
            )
            val options = QuizEngine.makeOptions(
                question = question,
                optionPool = KanaData.getAllPracticeItems(),
                random = Random(seed),
            )
            assertEquals(8, options.size)
            assertEquals(options.size, options.toSet().size)
            assertTrue(options.contains("シ"))
        }
    }

    @Test
    fun `builds a weighted question queue to the requested limit`() {
        val pool = listOf(KanaData.getItemByRomaji("a")!!, KanaData.getItemByRomaji("i")!!)
        val queue = QuizEngine.buildQuestionQueue(
            pool = pool,
            questionLimit = 5,
            getWeight = { 1.0 },
            makeQuestion = { item -> QuizEngine.makeQuestion(item, mode = CharMode.KATAKANA) },
            random = ZeroRandom,
        )

        assertEquals(5, queue.size)
        assertEquals(listOf("a", "i", "a", "i", "a"), queue.map { it.answer })
    }

    @Test
    fun `weighted shuffle respects heavy items and zero weight fallback`() {
        val items = listOf("light", "heavy")
        val weights = mapOf("light" to 0.0, "heavy" to 1.0)
        repeat(10) { seed ->
            val shuffled = QuizEngine.weightedShuffle(items, { weights.getValue(it) }, Random(seed))
            assertEquals(items.toSet(), shuffled.toSet())
            assertEquals("heavy", shuffled[0])
        }
        // 全零权重时回退为普通洗牌，仍包含全部元素
        val allZero = QuizEngine.weightedShuffle(items, { 0.0 }, ZeroRandom)
        assertEquals(items.toSet(), allZero.toSet())
    }

    @Test
    fun `empty pool or zero limit returns an empty queue`() {
        val makeQ: (com.konomip.kanatrainer.data.KanaItem) -> Question = {
            Question(it, "", "", AnswerType.ROMAJI, Script.HIRAGANA, "")
        }
        assertTrue(QuizEngine.buildQuestionQueue(emptyList(), 10, makeQuestion = makeQ).isEmpty())
        assertTrue(
            QuizEngine.buildQuestionQueue(
                KanaData.getAllPracticeItems(),
                0,
                makeQuestion = makeQ,
            ).isEmpty(),
        )
    }
}
