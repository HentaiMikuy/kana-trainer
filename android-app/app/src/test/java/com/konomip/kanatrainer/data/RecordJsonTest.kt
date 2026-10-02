package com.konomip.kanatrainer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordJsonTest {

    @Test
    fun `parses bare arrays like the web localStorage payload`() {
        val text = """[{"romaji":"shi","hiragana":"し","katakana":"シ","group":"s","attempts":3,"correct":2,"misses":1,"masteredStreak":2,"lastPracticedAt":"2026-06-02T00:00:00.000Z"}]"""
        val records = RecordJson.parseRecordsPayload(text)

        assertEquals(1, records.size)
        assertEquals("shi", records[0].romaji)
        assertEquals(2, records[0].correct)
        assertEquals("2026-06-02T00:00:00.000Z", records[0].lastPracticedAt)
    }

    @Test
    fun `parses export payloads and ignores unknown fields`() {
        val text = """
            {
              "schemaVersion": 1,
              "exportedAt": "2026-06-03T01:02:03.004Z",
              "records": [
                {"romaji":"ka","hiragana":"か","katakana":"カ","group":"k","attempts":2,"correct":1,"misses":1,"unknownField":true}
              ],
              "extraTopLevel": 1
            }
        """.trimIndent()
        val records = RecordJson.parseRecordsPayload(text)

        assertEquals(1, records.size)
        assertEquals("ka", records[0].romaji)
        assertEquals(1, records[0].misses)
    }

    @Test
    fun `storage round trip keeps records intact`() {
        val records = listOf(
            LearningRecord(
                romaji = "shi", hiragana = "し", katakana = "シ", group = "s",
                attempts = 3, correct = 2, misses = 1, masteredStreak = 1,
                lastPracticedAt = "2026-06-02T00:00:00.000Z",
            ),
        )
        val text = RecordJson.writeRecords(records)
        val parsed = RecordJson.readRecords(text)

        assertEquals(records, parsed)
    }

    @Test
    fun `export payload json matches the web export shape`() {
        val payload = LearningRecords.buildLearningExportData(
            listOf(LearningRecord("a", "あ", "ア", "a")),
            java.time.Instant.parse("2026-06-03T01:02:03.004Z"),
        )
        val text = RecordJson.writeExportPayload(payload)

        assertTrue(text.contains("\"schemaVersion\": 1"))
        assertTrue(text.contains("\"exportedAt\": \"2026-06-03T01:02:03.004Z\""))
        assertTrue(text.contains("\"romaji\": \"a\""))
        assertTrue(text.endsWith("\n"))
    }
}
