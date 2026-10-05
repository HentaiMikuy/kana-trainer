package com.konomip.kanatrainer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.KanaItem
import com.konomip.kanatrainer.data.LearningRecord
import com.konomip.kanatrainer.data.LearningRecords
import com.konomip.kanatrainer.data.RecordJson
import com.konomip.kanatrainer.data.RecordStore
import com.konomip.kanatrainer.data.RoundMistake
import com.konomip.kanatrainer.data.Script
import com.konomip.kanatrainer.data.WeakRow
import com.konomip.kanatrainer.logic.AnswerMode
import com.konomip.kanatrainer.logic.AnswerRules
import com.konomip.kanatrainer.logic.AnswerType
import com.konomip.kanatrainer.logic.CharMode
import com.konomip.kanatrainer.logic.Direction
import com.konomip.kanatrainer.logic.GlyphStyle
import com.konomip.kanatrainer.logic.NextAction
import com.konomip.kanatrainer.logic.PronunciationMode
import com.konomip.kanatrainer.logic.PracticePlanner
import com.konomip.kanatrainer.logic.PracticeSession
import com.konomip.kanatrainer.logic.PracticeType
import com.konomip.kanatrainer.logic.Question
import com.konomip.kanatrainer.logic.SessionType
import com.konomip.kanatrainer.logic.TrainerSettings
import com.konomip.kanatrainer.speech.KanaSpeaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** 应用内页面。 */
enum class Screen { SETTINGS, QUIZ, CHART, WEAK, GOJUON, UPDATE }

enum class FeedbackStyle { NEUTRAL, RIGHT, WRONG }

data class Feedback(
    val text: String,
    val style: FeedbackStyle = FeedbackStyle.NEUTRAL,
    val answer: String? = null,
)

data class MasteryState(val bucket: String, val label: String, val detail: String)

/** 完成页学习报告数据，对应 Web 版 renderCompletionReport 输入。 */
data class CompletionReport(
    val accuracy: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val averageTime: String,
    val roundMistakes: List<RoundMistake>,
    val weakRows: List<WeakRow>,
    val longTermMistakes: List<LearningRecord>,
) {
    val hasRoundMistakes: Boolean get() = roundMistakes.isNotEmpty()
    val hasLongTermMistakes: Boolean get() = longTermMistakes.isNotEmpty()
}

data class PendingImport(val records: List<LearningRecord>)

/** 一轮练习的完整状态。 */
data class RoundState(
    val activeSessionType: SessionType = SessionType.NORMAL,
    val pool: List<KanaItem> = emptyList(),
    val optionPool: List<KanaItem> = emptyList(),
    val queue: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val current: Question? = null,
    val currentOptions: List<String> = emptyList(),
    val selectedAnswer: Boolean = false,
    val lastAnswerCorrect: Boolean? = null,
    val chosenAnswer: String? = null,
    val answered: Int = 0,
    val correct: Int = 0,
    val streak: Int = 0,
    val totalAnswerTimeMs: Long = 0,
    val timeAnsweredCount: Int = 0,
    val roundMistakes: Map<String, RoundMistake> = emptyMap(),
    val questionLimit: Int = 20,
    val finished: Boolean = false,
    val report: CompletionReport? = null,
    val questionShownAtMs: Long = 0,
) {
    val accuracy: Int get() = PracticeSession.calculateAccuracy(answered, correct)
    val averageTimeText: String
        get() = PracticeSession.getAverageAnswerTimeText(totalAnswerTimeMs, timeAnsweredCount)
    val progressPercent: Double
        get() = PracticeSession.getProgressPercent(selectedAnswer, currentIndex, questionLimit)
}

data class AppUiState(
    val screen: Screen = Screen.SETTINGS,
    val settings: TrainerSettings = TrainerSettings(),
    val round: RoundState = RoundState(),
    val records: Map<String, LearningRecord> = emptyMap(),
    val mistakes: Map<String, LearningRecord> = emptyMap(),
    val feedback: Feedback? = null,
    val pendingImport: PendingImport? = null,
    val helpVisible: Boolean = false,
    val speakerReady: Boolean = false,
)

/**
 * 页面状态与跨模块流程编排，迁移自 app.js。
 */
class KanaTrainerViewModel(application: Application) : AndroidViewModel(application) {

    private val store = RecordStore(application)
    val speaker = KanaSpeaker(application)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val settings: TrainerSettings get() = _uiState.value.settings
    private val round: RoundState get() = _uiState.value.round

    init {
        viewModelScope.launch {
            val loaded = store.load()
            _uiState.update {
                it.copy(
                    records = loaded,
                    mistakes = LearningRecords.getLongTermMistakeRecords(loaded),
                )
            }
        }
    }

    private fun getRecord(item: KanaItem): LearningRecord? = _uiState.value.records[item.key]

    private fun sendMessage(text: String) {
        _messages.tryEmit(text)
    }

    // ---------- 页面导航 ----------

    fun navigate(screen: Screen) {
        _uiState.update { it.copy(screen = screen) }
    }

    fun goBack(): Boolean {
        val current = _uiState.value.screen
        if (current == Screen.SETTINGS) return false
        _uiState.update { it.copy(screen = Screen.SETTINGS, helpVisible = false) }
        return true
    }

    fun openHelp() = _uiState.update { it.copy(helpVisible = true) }

    fun closeHelp() = _uiState.update { it.copy(helpVisible = false) }

    // ---------- 设置更新 ----------

    fun updateSettings(transform: (TrainerSettings) -> TrainerSettings) {
        _uiState.update { it.copy(settings = transform(it.settings)) }
    }

    fun setMode(mode: CharMode) = updateSettings { it.copy(mode = mode) }

    fun setDirection(direction: Direction) = updateSettings { it.copy(direction = direction) }

    fun setAnswerMode(answerMode: AnswerMode) =
        updateSettings { it.copy(answerMode = answerMode) }

    fun setPracticeType(practiceType: PracticeType) =
        updateSettings { it.copy(practiceType = practiceType) }

    fun setConfusingSet(confusingSet: String) =
        updateSettings { it.copy(selectedConfusingSet = confusingSet) }

    fun setGlyphStyle(glyphStyle: GlyphStyle) =
        updateSettings { it.copy(glyphStyle = glyphStyle) }

    fun setPronunciationMode(pronunciationMode: PronunciationMode) =
        updateSettings { it.copy(pronunciationMode = pronunciationMode) }

    fun setChallengeEnabled(enabled: Boolean) =
        updateSettings { it.copy(challengeEnabled = enabled) }

    fun setChallengeLimitSeconds(seconds: Int) =
        updateSettings { it.copy(challengeLimitSeconds = seconds.coerceIn(5, 30)) }

    fun setConfiguredQuestionLimit(limit: Int) =
        updateSettings { it.copy(configuredQuestionLimit = limit.coerceIn(10, 50)) }

    fun setIncludeDakuten(include: Boolean) =
        updateSettings { it.copy(includeDakuten = include) }

    fun setIncludeSmallKana(include: Boolean) =
        updateSettings { it.copy(includeSmallKana = include) }

    /** 至少保留一行，与 Web 版 handleToggleRow 行为一致。 */
    fun toggleRow(rowId: String) = updateSettings { current ->
        val selected = current.selectedRows
        val next = if (selected.contains(rowId) && selected.size > 1) {
            selected - rowId
        } else {
            selected + rowId
        }
        current.copy(selectedRows = next)
    }

    fun toggleChartScript() = updateSettings { current ->
        current.copy(
            chartScript = if (current.chartScript == Script.KATAKANA) {
                Script.HIRAGANA
            } else {
                Script.KATAKANA
            },
        )
    }

    // ---------- 练习流程 ----------

    /** 开始新一轮：重置本轮统计并按当前设置重建题目队列，随后进入答题页。 */
    fun startQuiz() {
        val s = settings
        val pool = PracticePlanner.getActivePool(s)
        val optionPool = if (s.practiceType == PracticeType.CONFUSING) {
            KanaData.getAllPracticeItems()
        } else {
            pool
        }
        val activeSessionType = when (s.practiceType) {
            PracticeType.NORMAL -> SessionType.NORMAL
            PracticeType.CONFUSING -> SessionType.CONFUSING
        }
        val questionLimit = s.configuredQuestionLimit
        val queue = PracticePlanner.buildQueue(
            pool = pool,
            settings = s,
            questionLimit = questionLimit,
            activeSessionType = activeSessionType,
            getRecord = ::getRecord,
        )
        _uiState.update {
            it.copy(
                screen = Screen.QUIZ,
                round = RoundState(
                    activeSessionType = activeSessionType,
                    pool = pool,
                    optionPool = optionPool,
                    queue = queue,
                    questionLimit = questionLimit,
                ),
                feedback = null,
            )
        }
        renderQuestion(0)
    }

    /** 错题复习入口，迁移自 startMistakeReview。 */
    fun startMistakeReview(items: List<KanaItem>): Boolean {
        if (items.isEmpty()) {
            sendMessage("目前还没有错题。")
            return false
        }
        val s = settings
        val questionLimit = PracticeSession.getMistakeReviewQuestionLimit(items.size)
        val pool = PracticeSession.createChoicePool(items, KanaData.getAllPracticeItems())
        val queue = PracticePlanner.buildQueue(
            pool = items,
            settings = s,
            questionLimit = questionLimit,
            activeSessionType = SessionType.MISTAKES,
            getRecord = ::getRecord,
        )
        _uiState.update {
            it.copy(
                round = RoundState(
                    activeSessionType = SessionType.MISTAKES,
                    pool = items,
                    optionPool = pool,
                    queue = queue,
                    questionLimit = questionLimit,
                ),
                feedback = null,
                screen = Screen.QUIZ,
            )
        }
        renderQuestion(0)
        return true
    }

    fun reviewLongTermMistakes() {
        startMistakeReview(longTermMistakeItems())
    }

    fun reviewRoundMistakes() {
        startMistakeReview(round.roundMistakes.values.map { it.item })
    }

    private fun longTermMistakeItems(): List<KanaItem> =
        _uiState.value.mistakes.values.mapNotNull { KanaData.getCanonicalPracticeItem(it) }

    private fun renderQuestion(index: Int) {
        val r = _uiState.value.round
        val queue = r.queue
        if (index >= queue.size) {
            _uiState.update {
                it.copy(
                    round = r.copy(
                        currentIndex = index,
                        current = null,
                        currentOptions = emptyList(),
                        selectedAnswer = false,
                    ),
                    feedback = null,
                )
            }
            return
        }
        val current = queue[index]
        val optionPool = r.optionPool.ifEmpty { r.pool }
        val options = PracticePlanner.makeOptions(current, settings, optionPool)
        _uiState.update {
            it.copy(
                round = r.copy(
                    currentIndex = index,
                    current = current,
                    currentOptions = options,
                    selectedAnswer = false,
                    lastAnswerCorrect = null,
                    questionShownAtMs = System.currentTimeMillis(),
                ),
                feedback = null,
            )
        }
    }

    fun chooseAnswer(answer: String) {
        completeAnswer(answer)
    }

    fun revealAnswer() {
        if (round.selectedAnswer || round.current == null) return
        completeAnswer("", forceIncorrect = true)
    }

    fun submitInputAnswer(text: String) {
        val r = round
        val current = r.current
        if (settings.answerMode != AnswerMode.INPUT || r.selectedAnswer || current == null) return

        if (AnswerRules.normalizeKanaText(text).isEmpty()) {
            val label = if (current.answerType == AnswerType.ROMAJI) "罗马音" else "假名"
            _uiState.update { it.copy(feedback = Feedback("请输入$label。")) }
            return
        }
        completeAnswer(text)
    }

    /** 限时挑战倒计时归零，自动判错。 */
    fun onQuestionTimeout() {
        val r = round
        val current = r.current
        if (r.selectedAnswer || current == null) return
        completeAnswer(
            answer = "",
            forceIncorrect = true,
            presetFeedback = Feedback("时间到。正确答案是 ", FeedbackStyle.WRONG, current.answer),
        )
    }

    private fun completeAnswer(
        answer: String,
        forceIncorrect: Boolean = false,
        presetFeedback: Feedback? = null,
    ) {
        val r = round
        val current = r.current ?: return
        if (r.selectedAnswer) return

        val elapsedMs = (System.currentTimeMillis() - r.questionShownAtMs).coerceAtLeast(0)
        val isCorrect = !forceIncorrect && AnswerRules.isAnswerCorrect(current, answer)

        val feedback = presetFeedback ?: if (isCorrect) {
            Feedback("答对了。", FeedbackStyle.RIGHT)
        } else {
            Feedback("正确答案是 ", FeedbackStyle.WRONG, current.answer)
        }

        val result = LearningRecords.applyPracticeResult(
            _uiState.value.records,
            current.item,
            isCorrect,
        )
        val roundMistakes = if (isCorrect) {
            r.roundMistakes
        } else {
            rememberRoundMistake(r.roundMistakes, current.item, current.answerScript)
        }

        _uiState.update {
            it.copy(
                records = result.records,
                mistakes = LearningRecords.getLongTermMistakeRecords(result.records),
                feedback = feedback,
                round = r.copy(
                    selectedAnswer = true,
                    lastAnswerCorrect = isCorrect,
                    chosenAnswer = answer.ifEmpty { null },
                    answered = r.answered + 1,
                    correct = r.correct + if (isCorrect) 1 else 0,
                    streak = if (isCorrect) r.streak + 1 else 0,
                    totalAnswerTimeMs = r.totalAnswerTimeMs + elapsedMs,
                    timeAnsweredCount = r.timeAnsweredCount + 1,
                    roundMistakes = roundMistakes,
                ),
            )
        }

        viewModelScope.launch { store.save(_uiState.value.records) }

        if (settings.pronunciationMode == PronunciationMode.AUTO) {
            speakItem(current.item)
        }
    }

    private fun rememberRoundMistake(
        mistakes: Map<String, RoundMistake>,
        item: KanaItem,
        answerScript: Script?,
    ): Map<String, RoundMistake> {
        val existing = mistakes[item.key]
        val script = answerScript ?: existing?.answerScript ?: item.forcedScript
        return mistakes + (
            item.key to RoundMistake(
                item = item,
                answerScript = script,
                misses = (existing?.misses ?: 0) + 1,
            )
            )
    }

    fun nextQuestion() {
        val r = round
        when (PracticeSession.getNextQuestionAction(r.selectedAnswer, r.currentIndex, r.questionLimit)) {
            NextAction.BLOCKED -> return
            NextAction.FINISH -> finishQuiz()
            NextAction.ADVANCE -> renderQuestion(r.currentIndex + 1)
        }
    }

    private fun finishQuiz() {
        val r = round
        val (accuracy, wrongCount) = PracticeSession.getCompletionStats(r.answered, r.correct)
        val report = CompletionReport(
            accuracy = accuracy,
            correctCount = r.correct,
            wrongCount = wrongCount,
            averageTime = PracticeSession.getAverageAnswerTimeText(r.totalAnswerTimeMs, r.timeAnsweredCount),
            roundMistakes = KanaData.sortByMissPriorityRoundMistakes(r.roundMistakes.values.toList()),
            weakRows = KanaData.getWeakRows(r.roundMistakes.values.toList()),
            longTermMistakes = KanaData.sortByMissPriorityRecords(_uiState.value.mistakes.values.toList()),
        )
        _uiState.update {
            it.copy(
                round = r.copy(
                    finished = true,
                    report = report,
                    current = null,
                    currentOptions = emptyList(),
                ),
                feedback = null,
            )
        }
    }

    // ---------- 统计与展示数据 ----------

    fun getMasteryState(item: KanaItem): MasteryState {
        val record = getRecord(item) ?: return MasteryState("new", "新", "尚未练习")
        val attempts = maxOf(record.attempts, 0)
        val misses = maxOf(record.misses, 0)
        val accuracy = LearningRecords.getRecordAccuracy(record)

        return when {
            LearningRecords.isLongTermWeakRecord(record) ->
                MasteryState("weak", "弱", "$accuracy% · $misses 错")
            record.masteredStreak >= 3 && accuracy >= 90 ->
                MasteryState("steady", "稳", "$accuracy% · 连对 ${record.masteredStreak}")
            else ->
                MasteryState("practice", "练", "$accuracy% · $attempts 次")
        }
    }

    /** 容易忘的假名列表：按错次、答题次数排序，最多 8 条。 */
    fun getWeakList(): List<LearningRecord> =
        _uiState.value.mistakes.values
            .sortedWith(
                compareByDescending<LearningRecord> { it.misses }
                    .thenByDescending { it.attempts },
            )
            .take(8)

    // ---------- 发音 ----------

    fun speakCurrent() {
        round.current?.let { speakItem(it.item) }
    }

    fun speakItem(item: KanaItem) {
        speaker.speak(item.hiragana, item.audioKey)
    }

    // ---------- 学习数据动作 ----------

    fun exportRecords(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val payload = LearningRecords.buildLearningExportData(_uiState.value.records.values)
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?.use { output ->
                            output.write(RecordJson.writeExportPayload(payload).toByteArray(Charsets.UTF_8))
                        }
                        ?: error("无法打开导出目标")
                }.isSuccess
            }
            sendMessage(if (ok) "学习记录已导出。" else "导出学习记录失败。")
        }
    }

    fun importRecords(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.readText()
                }.getOrNull()
            }
            val parsed = text?.let { content ->
                runCatching { RecordJson.parseRecordsPayload(content) }.getOrNull()
            }
            when {
                parsed == null ->
                    sendMessage("导入失败，请选择有效的 JSON 学习数据文件。")
                else -> _uiState.update { it.copy(pendingImport = PendingImport(parsed)) }
            }
        }
    }

    /** 确认导入：合并并覆盖同一假名的历史记录。 */
    fun confirmImport() {
        val pending = _uiState.value.pendingImport ?: return
        val result = LearningRecords.mergeLearningRecords(_uiState.value.records, pending.records)
        _uiState.update {
            it.copy(
                records = result.records,
                mistakes = LearningRecords.getLongTermMistakeRecords(result.records),
                pendingImport = null,
            )
        }
        viewModelScope.launch {
            store.save(_uiState.value.records)
            sendMessage(
                if (result.importedCount == 0) {
                    "没有找到可导入的学习记录。"
                } else {
                    "已导入 ${result.importedCount} 条学习记录。"
                },
            )
        }
    }

    fun dismissImport() = _uiState.update { it.copy(pendingImport = null) }

    fun clearRecords() {
        viewModelScope.launch {
            store.clear()
            _uiState.update { it.copy(records = emptyMap(), mistakes = emptyMap()) }
            sendMessage("本地学习记录已清空。")
        }
    }

    override fun onCleared() {
        speaker.shutdown()
        super.onCleared()
    }
}
