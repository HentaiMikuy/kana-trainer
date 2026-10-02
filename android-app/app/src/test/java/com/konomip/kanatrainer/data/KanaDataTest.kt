package com.konomip.kanatrainer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class KanaDataTest {

    @Test
    fun `builds the complete practice item list`() {
        val items = KanaData.getAllPracticeItems()
        assertEquals(104, items.size)

        val shi = KanaData.getItemByRomaji("shi")
        assertEquals(KanaItem("shi", "し", "シ", "s"), shi)
    }

    @Test
    fun `chart sections cover base dakuten semi and small groups`() {
        val sections = KanaData.getChartSections().associateBy { it.key }
        assertEquals(4, sections.size)
        assertEquals(46, sections.getValue("base").items.size)
        assertEquals(20, sections.getValue("dakuten").items.size)
        assertEquals(5, sections.getValue("semi").items.size)
        assertEquals(33, sections.getValue("small").items.size)
        // 浊音区按行字母分组，标签为「か行」等
        val dakutenRows = KanaData.groupChartItemsByRow(sections.getValue("dakuten").items)
        assertEquals("か行", dakutenRows.first().label)
    }

    @Test
    fun `builds confusing pools with the expected script`() {
        val pool = KanaData.getConfusingPool("nu-me")
        assertEquals(2, pool.size)
        assertTrue(pool.all { it.forcedScript == Script.HIRAGANA })
        assertEquals(listOf("ぬ", "め"), pool.map { it.hiragana })

        // 全部分组合并后按假名去重
        val allPool = KanaData.getConfusingPool("all")
        assertEquals(13, allPool.size)
        assertTrue(allPool.all { it.forcedScript != null })
    }

    @Test
    fun `row and group labels follow the web version`() {
        assertEquals("さ", KanaData.getRowName("s"))
        assertEquals("さ行", KanaData.getGroupLabel("s"))
        assertEquals("浊音", KanaData.getRowName("dakuten"))
        assertEquals("浊音组", KanaData.getGroupLabel("dakuten"))
        assertEquals("拗音组", KanaData.getGroupLabel("small"))
    }

    @Test
    fun `weak rows aggregate misses by group`() {
        val mistakes = listOf(
            RoundMistake(KanaData.getItemByRomaji("ka")!!, misses = 2),
            RoundMistake(KanaData.getItemByRomaji("ki")!!, misses = 1),
            RoundMistake(KanaData.getItemByRomaji("sa")!!, misses = 3),
        )
        val rows = KanaData.getWeakRows(mistakes)
        assertEquals(2, rows.size)
        // 并列时按假名数量降序：か行 2 个在前
        assertEquals("か行", rows[0].label)
        assertEquals(3, rows[0].misses)
        assertEquals(2, rows[0].count)
        assertEquals("さ行", rows[1].label)
        assertEquals(3, rows[1].misses)
        assertEquals(1, rows[1].count)
    }

    @Test
    fun `canonical item lookup drops unknown records`() {
        val known = KanaData.getCanonicalPracticeItem(
            LearningRecord(romaji = "shi", hiragana = "し", katakana = "シ", group = "wrong"),
        )
        assertEquals("s", known?.group)
        assertNull(KanaData.getCanonicalPracticeItem(LearningRecord("xx", "yy", "zz")))
    }

    @Test
    fun `shuffle does not mutate the input`() {
        val source = listOf(1, 2, 3, 4)
        val shuffled = KanaData.shuffle(source, Random(7))
        assertEquals(listOf(1, 2, 3, 4), source)
        assertEquals(listOf(1, 2, 3, 4).sorted(), shuffled.sorted())
    }

    @Test
    fun `sort by miss priority orders records for the report`() {
        val low = LearningRecord("a", "あ", "ア", "a", attempts = 4, misses = 1)
        val high = LearningRecord("i", "い", "イ", "a", attempts = 5, misses = 3)
        val sorted = KanaData.sortByMissPriorityRecords(listOf(low, high))
        assertEquals("i", sorted[0].romaji)
    }

    @Test
    fun `withQuestionScript copies the item`() {
        val item = KanaData.getItemByRomaji("nu")!!
        val forced = KanaData.withQuestionScript(item, Script.HIRAGANA)
        assertEquals(Script.HIRAGANA, forced.forcedScript)
        assertFalse(item.forcedScript != null)
        assertEquals(item.key, forced.key)
    }
}
