package com.konomip.kanatrainer.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** 五十音图经典网格（KanaData.getGojuonSections）的结构测试，与 Web 版 gojuon-chart 行为对齐。 */
class GojuonSectionsTest {

    private val sections = KanaData.getGojuonSections()

    private fun romajis(row: GojuonRow): List<String?> = row.slots.map { it?.romaji }

    @Test
    fun `row ids are unique within each section for lazy list reuse`() {
        // な行与单独的ん曾共用 n，导致滚动预加载或复用时发生重复 key 崩溃。
        sections.forEach { section ->
            val ids = section.rows.map { it.id }
            assertEquals("${section.title} contains duplicate row ids: $ids", ids.size, ids.toSet().size)
        }
    }

    @Test
    fun `builds four sections with classic columns`() {
        assertEquals(listOf("base", "dakuten", "semi", "small"), sections.map { it.key })
        assertEquals(listOf("清音", "浊音", "半浊音", "拗音"), sections.map { it.title })

        val (base, dakuten, semi, small) = sections
        assertEquals(listOf("a", "i", "u", "e", "o"), base.columns)
        assertEquals(listOf("a", "i", "u", "e", "o"), dakuten.columns)
        assertEquals(listOf("a", "i", "u", "e", "o"), semi.columns)
        assertEquals(listOf("a", "u", "o"), small.columns)
    }

    @Test
    fun `base section places kana into vowel columns with classic blanks`() {
        val base = sections[0]
        assertEquals(11, base.rows.size)

        assertEquals("あ行", base.rows[0].label)
        assertEquals(listOf("a", "i", "u", "e", "o"), romajis(base.rows[0]))

        // や行只有 a / u / o 三列
        assertEquals("や行", base.rows[7].label)
        assertEquals(listOf("ya", null, "yu", null, "yo"), romajis(base.rows[7]))

        // わ行：わ 在 a 列、を 在 o 列
        assertEquals("わ行", base.rows[9].label)
        assertEquals(listOf("wa", null, null, null, "wo"), romajis(base.rows[9]))

        // ん 单独一行，放在最后一列
        assertEquals("ん", base.rows[10].label)
        assertEquals(listOf(null, null, null, null, "n"), romajis(base.rows[10]))
    }

    @Test
    fun `dakuten and semi sections split into rows by occupied column`() {
        val dakuten = sections[1]
        assertEquals(listOf("が行", "ざ行", "だ行", "ば行"), dakuten.rows.map { it.label })
        assertEquals(listOf("ga", "gi", "gu", "ge", "go"), romajis(dakuten.rows[0]))
        // ぢ / づ 要回到 だ行 的 i / u 列，而不是另起新行
        assertEquals(listOf("da", "ji/di", "zu/du", "de", "do"), romajis(dakuten.rows[2]))

        val semi = sections[2]
        assertEquals(1, semi.rows.size)
        assertEquals("ぱ行", semi.rows[0].label)
        assertEquals(listOf("pa", "pi", "pu", "pe", "po"), romajis(semi.rows[0]))
    }

    @Test
    fun `small kana section uses three columns`() {
        val small = sections[3]
        assertEquals(11, small.rows.size)
        assertEquals("きゃ行", small.rows[0].label)
        assertEquals(listOf("kya", "kyu", "kyo"), romajis(small.rows[0]))
        assertEquals("ぴゃ行", small.rows[10].label)
        assertEquals(listOf("pya", "pyu", "pyo"), romajis(small.rows[10]))
    }

    @Test
    fun `grid covers every practice item exactly once`() {
        val counts = sections.map { section ->
            section.rows.sumOf { row -> row.slots.count { it != null } }
        }
        assertEquals(listOf(46, 20, 5, 33), counts)
        assertEquals(KanaData.getAllPracticeItems().size, counts.sum())
    }
}
