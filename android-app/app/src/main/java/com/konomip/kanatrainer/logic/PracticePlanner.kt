package com.konomip.kanatrainer.logic

import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.KanaItem
import com.konomip.kanatrainer.data.LearningRecord
import com.konomip.kanatrainer.data.RoundMistake
import kotlin.random.Random

/**
 * 练习规划：练习标签、练习池、易混淆干扰项与题目队列，迁移自 practice-planner.js。
 * 通过 [getRecord] 注入长期学习记录，保持纯逻辑可测试。
 */
object PracticePlanner {

    fun getConfusingSetLabel(selectedConfusingSet: String = "all"): String =
        if (selectedConfusingSet == "all") {
            "全部易混淆"
        } else {
            KanaData.getConfusingSetById(selectedConfusingSet)?.label ?: "易混淆专项"
        }

    fun getModeLabel(settings: TrainerSettings): String = when {
        settings.practiceType == PracticeType.CONFUSING -> "假名"
        settings.mode == CharMode.KATAKANA -> "片假名"
        settings.mode == CharMode.HIRAGANA -> "平假名"
        else -> "混合"
    }

    fun getPracticeLabel(settings: TrainerSettings, activeSessionType: SessionType): String =
        when (activeSessionType) {
            SessionType.MISTAKES -> "错题复习"
            SessionType.CONFUSING ->
                "易混淆 · ${getConfusingSetLabel(settings.selectedConfusingSet)}"
            SessionType.NORMAL -> getModeLabel(settings)
        }

    fun getPool(settings: TrainerSettings): List<KanaItem> =
        QuizEngine.getPracticePool(
            selectedRows = settings.selectedRows,
            includeDakuten = settings.includeDakuten,
            includeSmallKana = settings.includeSmallKana,
        )

    fun getConfusingPool(selectedConfusingSet: String): List<KanaItem> =
        KanaData.getConfusingPool(selectedConfusingSet)

    fun getActivePool(settings: TrainerSettings): List<KanaItem> =
        if (settings.practiceType == PracticeType.CONFUSING) {
            getConfusingPool(settings.selectedConfusingSet)
        } else {
            getPool(settings)
        }

    /** 易混淆专项下的优先干扰项：与当前假名同分组的其它成员。 */
    fun getConfusingDistractors(question: Question, settings: TrainerSettings): List<KanaItem> {
        if (settings.practiceType != PracticeType.CONFUSING) return emptyList()

        return KanaData.getSelectedConfusingSets(settings.selectedConfusingSet)
            .filter { it.items.contains(question.item.romaji) }
            .flatMap { set ->
                set.items
                    .mapNotNull { KanaData.getItemByRomaji(it) }
                    .filter { it.key != question.item.key }
                    .map { KanaData.withQuestionScript(it, set.script) }
            }
    }

    fun getReviewWeight(item: KanaItem, getRecord: (KanaItem) -> LearningRecord?): Double =
        ReviewWeight.calculate(item, getRecord(item))

    fun makeQuestion(
        item: KanaItem,
        settings: TrainerSettings,
        activeSessionType: SessionType,
        random: Random = Random.Default,
    ): Question =
        QuizEngine.makeQuestion(
            item = item,
            direction = settings.direction,
            mode = settings.mode,
            activeSessionType = activeSessionType,
            modeLabel = getModeLabel(settings),
            random = random,
        )

    fun makeOptions(
        question: Question,
        settings: TrainerSettings,
        optionPool: List<KanaItem>,
        random: Random = Random.Default,
    ): List<String> =
        QuizEngine.makeOptions(
            question = question,
            optionPool = optionPool,
            priorityDistractors = getConfusingDistractors(question, settings),
            random = random,
        )

    fun buildQueue(
        pool: List<KanaItem>,
        settings: TrainerSettings,
        questionLimit: Int,
        activeSessionType: SessionType,
        getRecord: (KanaItem) -> LearningRecord?,
        random: Random = Random.Default,
    ): List<Question> =
        QuizEngine.buildQuestionQueue(
            pool = pool,
            questionLimit = questionLimit,
            getWeight = { item -> getReviewWeight(item, getRecord) },
            makeQuestion = { item -> makeQuestion(item, settings, activeSessionType, random) },
            random = random,
        )
}
