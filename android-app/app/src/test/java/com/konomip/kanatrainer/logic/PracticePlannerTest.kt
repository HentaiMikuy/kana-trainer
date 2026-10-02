package com.konomip.kanatrainer.logic

import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.LearningRecord
import com.konomip.kanatrainer.data.Script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticePlannerTest {

    private val noRecords: (com.konomip.kanatrainer.data.KanaItem) -> LearningRecord? = { null }

    @Test
    fun `mode labels follow practice type and character mode`() {
        assertEquals("片假名", PracticePlanner.getModeLabel(TrainerSettings()))
        assertEquals(
            "平假名",
            PracticePlanner.getModeLabel(TrainerSettings(mode = CharMode.HIRAGANA)),
        )
        assertEquals(
            "混合",
            PracticePlanner.getModeLabel(TrainerSettings(mode = CharMode.MIXED)),
        )
        assertEquals(
            "假名",
            PracticePlanner.getModeLabel(TrainerSettings(practiceType = PracticeType.CONFUSING)),
        )
    }

    @Test
    fun `practice labels cover normal confusing and mistake sessions`() {
        assertEquals("片假名", PracticePlanner.getPracticeLabel(TrainerSettings(), SessionType.NORMAL))
        assertEquals(
            "易混淆 · 全部易混淆",
            PracticePlanner.getPracticeLabel(
                TrainerSettings(practiceType = PracticeType.CONFUSING),
                SessionType.CONFUSING,
            ),
        )
        assertEquals(
            "易混淆 · シ / ツ",
            PracticePlanner.getPracticeLabel(
                TrainerSettings(practiceType = PracticeType.CONFUSING, selectedConfusingSet = "shi-tsu"),
                SessionType.CONFUSING,
            ),
        )
        assertEquals(
            "错题复习",
            PracticePlanner.getPracticeLabel(TrainerSettings(), SessionType.MISTAKES),
        )
    }

    @Test
    fun `active pool switches between normal and confusing practice`() {
        val normalPool = PracticePlanner.getActivePool(
            TrainerSettings(selectedRows = setOf("k"), includeDakuten = true, includeSmallKana = true),
        )
        assertEquals(16, normalPool.size)

        val confusingPool = PracticePlanner.getActivePool(
            TrainerSettings(practiceType = PracticeType.CONFUSING, selectedConfusingSet = "shi-tsu"),
        )
        assertEquals(2, confusingPool.size)
        assertTrue(confusingPool.all { it.forcedScript == Script.KATAKANA })
    }

    @Test
    fun `confusing distractors contain same set members`() {
        val settings = TrainerSettings(practiceType = PracticeType.CONFUSING)
        val question = QuizEngine.makeQuestion(
            item = KanaData.getItemByRomaji("shi")!!,
            direction = Direction.ROMAJI_TO_KANA,
            mode = CharMode.KATAKANA,
        )
        val distractors = PracticePlanner.getConfusingDistractors(question, settings)
        assertEquals(listOf("tsu"), distractors.map { it.romaji })
        assertEquals(Script.KATAKANA, distractors[0].forcedScript)

        // 普通练习没有优先干扰项
        assertEquals(
            emptyList<com.konomip.kanatrainer.data.KanaItem>(),
            PracticePlanner.getConfusingDistractors(
                question,
                TrainerSettings(),
            ),
        )
    }

    @Test
    fun `build queue uses configured limit and review weights`() {
        val settings = TrainerSettings()
        val queue = PracticePlanner.buildQueue(
            pool = PracticePlanner.getPool(settings),
            settings = settings,
            questionLimit = settings.configuredQuestionLimit,
            activeSessionType = SessionType.NORMAL,
            getRecord = noRecords,
        )
        assertEquals(settings.configuredQuestionLimit, queue.size)
    }

    @Test
    fun `review weight mirrors the core calculation`() {
        val item = KanaData.getItemByRomaji("shi")!!
        assertEquals(
            ReviewWeight.calculate(item, null),
            PracticePlanner.getReviewWeight(item, noRecords),
            1e-9,
        )
    }
}
