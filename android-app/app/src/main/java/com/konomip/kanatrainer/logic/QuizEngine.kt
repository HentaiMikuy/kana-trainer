package com.konomip.kanatrainer.logic

import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.KanaItem
import com.konomip.kanatrainer.data.RawKana
import com.konomip.kanatrainer.data.Script
import kotlin.random.Random

enum class AnswerType { ROMAJI, KANA }

/** 单道题目，字段与 Web 版 makeQuestion 产物一致。 */
data class Question(
    val item: KanaItem,
    val prompt: String,
    val answer: String,
    val answerType: AnswerType,
    val answerScript: Script,
    val helper: String,
)

data class KanaChoice(val kana: String, val script: Script)

/** 出题引擎，迁移自 quiz-engine.js。 */
object QuizEngine {

    /** 根据选中行和开关生成练习池，顺序与 Web 版一致：基础行在前，浊音、拗音在后。 */
    fun getPracticePool(
        selectedRows: Set<String>,
        includeDakuten: Boolean = false,
        includeSmallKana: Boolean = false,
    ): List<KanaItem> {
        val baseItems = KanaData.BASE_ROWS
            .filter { selectedRows.contains(it.id) }
            .flatMap { row -> row.items.map { KanaData.toItem(it, row.id) } }
        val extras = mutableListOf<KanaItem>()

        if (includeDakuten) {
            extras += KanaData.DAKUTEN_ITEMS
                .filter { selectedRows.contains(it.group ?: "") }
                .map { KanaData.toItem(it, "dakuten") }
        }

        if (includeSmallKana) {
            extras += KanaData.SMALL_KANA_ITEMS
                .filter { selectedRows.contains(it.group ?: "") }
                .map { KanaData.toItem(it, "small") }
        }

        return baseItems + extras
    }

    fun getKanaChoice(item: KanaItem, mode: CharMode, random: Random): KanaChoice {
        item.forcedScript?.let { forced ->
            val kana = if (forced == Script.HIRAGANA) item.hiragana else item.katakana
            return KanaChoice(kana, forced)
        }
        return when (mode) {
            CharMode.HIRAGANA -> KanaChoice(item.hiragana, Script.HIRAGANA)
            CharMode.KATAKANA -> KanaChoice(item.katakana, Script.KATAKANA)
            CharMode.MIXED ->
                if (random.nextDouble() > 0.5) {
                    KanaChoice(item.katakana, Script.KATAKANA)
                } else {
                    KanaChoice(item.hiragana, Script.HIRAGANA)
                }
        }
    }

    fun makeQuestion(
        item: KanaItem,
        direction: Direction = Direction.KANA_TO_ROMAJI,
        mode: CharMode = CharMode.KATAKANA,
        activeSessionType: SessionType = SessionType.NORMAL,
        modeLabel: String = "假名",
        random: Random = Random.Default,
    ): Question {
        val kanaChoice = getKanaChoice(item, mode, random)
        val helperPrefix = when (activeSessionType) {
            SessionType.CONFUSING -> "易混淆专项："
            SessionType.MISTAKES -> "错题复习："
            SessionType.NORMAL -> ""
        }

        return if (direction == Direction.KANA_TO_ROMAJI) {
            Question(
                item = item,
                prompt = kanaChoice.kana,
                answer = item.romaji,
                answerType = AnswerType.ROMAJI,
                answerScript = kanaChoice.script,
                helper = "${helperPrefix}选择对应的罗马音",
            )
        } else {
            Question(
                item = item,
                prompt = item.romaji,
                answer = kanaChoice.kana,
                answerType = AnswerType.KANA,
                answerScript = kanaChoice.script,
                helper = "${helperPrefix}选择对应的$modeLabel",
            )
        }
    }

    fun getOptionValue(item: KanaItem, question: Question): String {
        if (question.answerType == AnswerType.ROMAJI) return item.romaji
        val script = item.forcedScript ?: question.answerScript
        return if (script == Script.HIRAGANA) item.hiragana else item.katakana
    }

    /**
     * 生成选择题选项：易混淆干扰项优先，去重后截断到 maxOptions，整体洗牌。
     * shuffle 可注入，便于测试（对应 Web 版的依赖注入风格）。
     */
    fun makeOptions(
        question: Question,
        optionPool: List<KanaItem> = emptyList(),
        priorityDistractors: List<KanaItem> = emptyList(),
        maxOptions: Int = 8,
        random: Random = Random.Default,
        shuffle: (List<String>, Random) -> List<String> = { items, r -> KanaData.shuffle(items, r) },
    ): List<String> {
        val priorityCandidates = priorityDistractors
            .map { getOptionValue(it, question) }
            .filter { it != question.answer }
        val generalCandidates = optionPool
            .filter { it.romaji != question.item.romaji || getOptionValue(it, question) != question.answer }
            .map { getOptionValue(it, question) }
        val uniqueCandidates = (priorityCandidates + shuffle(generalCandidates, random))
            .distinct()
            .filter { it != question.answer }

        return shuffle(listOf(question.answer) + uniqueCandidates.take(maxOptions - 1), random)
    }

    /** 加权洗牌，与 Web 版 weightedShuffle 保持同一算法。 */
    fun <T> weightedShuffle(
        items: List<T>,
        getWeight: (T) -> Double,
        random: Random = Random.Default,
    ): List<T> {
        data class Candidate<T>(val item: T, val weight: Double)

        val candidates = items
            .map { Candidate(it, maxOf(getWeight(it), 0.0)) }
            .toMutableList()
        val result = mutableListOf<T>()

        while (candidates.isNotEmpty()) {
            val totalWeight = candidates.sumOf { it.weight }
            if (totalWeight <= 0.0) {
                result += KanaData.shuffle(candidates.map { it.item }, random)
                break
            }

            var cursor = random.nextDouble() * totalWeight
            var selectedIndex = -1
            for ((index, candidate) in candidates.withIndex()) {
                cursor -= candidate.weight
                if (cursor <= 0) {
                    selectedIndex = index
                    break
                }
            }
            val index = if (selectedIndex == -1) candidates.size - 1 else selectedIndex
            result += candidates.removeAt(index).item
        }

        return result
    }

    /** 按复习权重循环填充题目队列，直到达到题量上限。 */
    fun buildQuestionQueue(
        pool: List<KanaItem>,
        questionLimit: Int,
        getWeight: (KanaItem) -> Double = { 1.0 },
        makeQuestion: (KanaItem) -> Question,
        random: Random = Random.Default,
    ): List<Question> {
        val limit = maxOf(questionLimit, 0)
        val rounds = mutableListOf<KanaItem>()

        if (pool.isEmpty() || limit == 0) return emptyList()

        while (rounds.size < limit) {
            rounds += weightedShuffle(pool, getWeight, random)
        }

        return rounds.take(limit).map(makeQuestion)
    }
}
