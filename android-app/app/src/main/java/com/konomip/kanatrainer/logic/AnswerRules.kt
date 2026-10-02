package com.konomip.kanatrainer.logic

import com.konomip.kanatrainer.data.Script

/** 判题规则，迁移自 kana-core.js 的答案兼容逻辑。 */
object AnswerRules {

    /** 罗马音规范化：去首尾空白、转小写、移除内部空白、统一各类连字符为半角。 */
    fun normalizeAnswerText(value: String): String = value
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), "")
        .replace(Regex("[‐‑‒–—−]"), "-")

    fun normalizeKanaText(value: String): String = value.trim()

    fun getAcceptedAnswers(question: Question): List<String> = when (question.answerType) {
        AnswerType.ROMAJI -> {
            val variants = if (question.item.romaji.contains("/")) {
                question.item.romaji.split("/")
            } else {
                listOf(question.item.romaji)
            }
            variants.map { normalizeAnswerText(it) }.distinct()
        }
        AnswerType.KANA -> listOf(
            normalizeKanaText(question.item.hiragana),
            normalizeKanaText(question.item.katakana),
        )
    }

    fun isAnswerCorrect(question: Question, userAnswer: String): Boolean =
        when (question.answerType) {
            AnswerType.ROMAJI ->
                getAcceptedAnswers(question).contains(normalizeAnswerText(userAnswer))
            AnswerType.KANA ->
                getAcceptedAnswers(question).contains(normalizeKanaText(userAnswer))
        }

    /** 题干当前显示脚本与强制脚本一致时使用；供发音等场景取平假名。 */
    fun questionKana(item: com.konomip.kanatrainer.data.KanaItem, script: Script): String =
        if (script == Script.HIRAGANA) item.hiragana else item.katakana
}
