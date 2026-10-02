package com.konomip.kanatrainer.logic

import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.Script

/** 测试字符模式。 */
enum class CharMode(val id: String) {
    KATAKANA("katakana"),
    HIRAGANA("hiragana"),
    MIXED("mixed");

    companion object {
        fun fromId(id: String): CharMode = entries.firstOrNull { it.id == id } ?: KATAKANA
    }
}

/** 出题方向。 */
enum class Direction(val id: String) {
    KANA_TO_ROMAJI("kana-to-romaji"),
    ROMAJI_TO_KANA("romaji-to-kana");

    companion object {
        fun fromId(id: String): Direction =
            entries.firstOrNull { it.id == id } ?: KANA_TO_ROMAJI
    }
}

/** 答题方式。 */
enum class AnswerMode(val id: String) {
    CHOICE("choice"),
    INPUT("input");

    companion object {
        fun fromId(id: String): AnswerMode = entries.firstOrNull { it.id == id } ?: CHOICE
    }
}

/** 练习类型与实际生效的会话类型。 */
enum class PracticeType(val id: String) {
    NORMAL("normal"),
    CONFUSING("confusing");

    companion object {
        fun fromId(id: String): PracticeType = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

enum class SessionType {
    NORMAL,
    CONFUSING,
    MISTAKES,
}

enum class GlyphStyle(val id: String) {
    PRINT("print"),
    HAND("hand");

    companion object {
        fun fromId(id: String): GlyphStyle = entries.firstOrNull { it.id == id } ?: PRINT
    }
}

enum class PronunciationMode(val id: String) {
    MANUAL("manual"),
    AUTO("auto");

    companion object {
        fun fromId(id: String): PronunciationMode =
            entries.firstOrNull { it.id == id } ?: MANUAL
    }
}

/**
 * 练习设置，默认值与 Web 版 app.js 的初始 state 一致。
 */
data class TrainerSettings(
    val mode: CharMode = CharMode.KATAKANA,
    val direction: Direction = Direction.KANA_TO_ROMAJI,
    val selectedRows: Set<String> = KanaData.BASE_ROWS.map { it.id }.toSet(),
    val configuredQuestionLimit: Int = 20,
    val includeDakuten: Boolean = true,
    val includeSmallKana: Boolean = false,
    val practiceType: PracticeType = PracticeType.NORMAL,
    val selectedConfusingSet: String = "all",
    val challengeEnabled: Boolean = false,
    val challengeLimitSeconds: Int = 15,
    val glyphStyle: GlyphStyle = GlyphStyle.PRINT,
    val pronunciationMode: PronunciationMode = PronunciationMode.MANUAL,
    val answerMode: AnswerMode = AnswerMode.CHOICE,
    val chartScript: Script = Script.KATAKANA,
)
