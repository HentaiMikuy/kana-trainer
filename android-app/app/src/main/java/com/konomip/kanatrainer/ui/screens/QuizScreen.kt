package com.konomip.kanatrainer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konomip.kanatrainer.data.KanaItem
import com.konomip.kanatrainer.data.Script
import com.konomip.kanatrainer.data.WeakRow
import com.konomip.kanatrainer.logic.AnswerMode
import com.konomip.kanatrainer.logic.AnswerType
import com.konomip.kanatrainer.logic.CharMode
import com.konomip.kanatrainer.logic.PracticePlanner
import com.konomip.kanatrainer.logic.PracticeSession
import com.konomip.kanatrainer.logic.SessionType
import com.konomip.kanatrainer.logic.TrainerSettings
import com.konomip.kanatrainer.ui.FeedbackStyle
import com.konomip.kanatrainer.ui.KanaTrainerViewModel
import com.konomip.kanatrainer.ui.components.KanaText
import com.konomip.kanatrainer.ui.components.StatStrip
import com.konomip.kanatrainer.ui.edgeToEdgeRoot
import com.konomip.kanatrainer.ui.theme.LocalKanaColors
import com.konomip.kanatrainer.ui.theme.LocalKanaFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** 依据强制脚本、答题脚本和设置推断假名展示脚本，迁移自 getDisplayScript。 */
fun displayScript(item: KanaItem, answerScript: Script?, settings: TrainerSettings): Script {
    item.forcedScript?.let { return it }
    answerScript?.let { return it }
    return when (settings.mode) {
        CharMode.HIRAGANA -> Script.HIRAGANA
        CharMode.KATAKANA -> Script.KATAKANA
        CharMode.MIXED -> settings.chartScript
    }
}

/** 返回 (主显示, 副显示) 一对假名，对应 Web 版 getKanaDisplay。 */
fun kanaDisplay(
    item: KanaItem,
    answerScript: Script?,
    settings: TrainerSettings,
): Pair<String, String> {
    val script = displayScript(item, answerScript, settings)
    return if (script == Script.HIRAGANA) {
        item.hiragana to item.katakana
    } else {
        item.katakana to item.hiragana
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(viewModel: KanaTrainerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val round = state.round
    val settings = state.settings
    val current = round.current
    val speakerAvailable by viewModel.speaker.available.collectAsState()

    Column(modifier = modifier.edgeToEdgeRoot().fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                TextButton(onClick = { viewModel.goBack() }) { Text("退出") }
            },
            title = {
                Column {
                    Text(
                        text = PracticePlanner.getPracticeLabel(settings, round.activeSessionType),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "第 ${round.currentIndex + 1} / ${round.questionLimit} 题",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                StatStrip(
                    answered = round.answered,
                    accuracy = round.accuracy,
                    streak = round.streak,
                    averageTime = round.averageTimeText,
                )
            }

            item {
                LinearProgressIndicator(
                    progress = { (round.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (settings.challengeEnabled && !round.finished) {
                item {
                    ChallengeTimerStrip(
                        limitSeconds = settings.challengeLimitSeconds,
                        shownAtMs = round.questionShownAtMs,
                        questionKey = current?.item?.key,
                        frozen = round.selectedAnswer,
                        onTimeout = { viewModel.onQuestionTimeout() },
                    )
                }
            }

            if (round.finished) {
                item {
                    CompletionReportCard(viewModel = viewModel, settings = settings)
                }
            } else {
                item {
                    QuestionCard(
                        viewModel = viewModel,
                        state = state,
                        speakerAvailable = speakerAvailable,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    viewModel: KanaTrainerViewModel,
    state: com.konomip.kanatrainer.ui.AppUiState,
    speakerAvailable: Boolean,
) {
    val round = state.round
    val settings = state.settings
    val current = round.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (current == null) {
                KanaText(
                    text = "無",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "当前范围没有可练习的假名",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (current.answerType == AnswerType.ROMAJI) {
                        KanaText(
                            text = current.prompt,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 84.sp),
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            text = current.prompt,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = current.helper,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (settings.answerMode == AnswerMode.CHOICE) {
                    OptionGrid(
                        options = round.currentOptions,
                        isKanaAnswer = current.answerType == AnswerType.KANA,
                        selectedAnswer = round.selectedAnswer,
                        chosenAnswer = round.chosenAnswer,
                        correctAnswer = current.answer,
                        onChoose = { viewModel.chooseAnswer(it) },
                    )
                } else {
                    InputAnswerArea(
                        enabled = !round.selectedAnswer,
                        isKanaAnswer = current.answerType == AnswerType.KANA,
                        selectedAnswer = round.selectedAnswer,
                        lastAnswerCorrect = round.lastAnswerCorrect,
                        onSubmit = { viewModel.submitInputAnswer(it) },
                        questionKey = "${round.currentIndex}-${current.item.key}",
                    )
                }

                state.feedback?.let { feedback ->
                    FeedbackText(
                        text = feedback.text,
                        answer = feedback.answer,
                        style = feedback.style,
                        isKanaAnswer = current.answerType == AnswerType.KANA,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { viewModel.speakCurrent() },
                        enabled = speakerAvailable,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("发音")
                    }
                    OutlinedButton(
                        onClick = { viewModel.revealAnswer() },
                        enabled = !round.selectedAnswer,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("看答案")
                    }
                    Button(
                        onClick = { viewModel.nextQuestion() },
                        enabled = round.selectedAnswer,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (round.currentIndex + 1 == round.questionLimit) {
                                "查看结果"
                            } else {
                                "下一题"
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 限时挑战倒计时条，对应 Web 版 challenge-timer.js 的展示逻辑。 */
@Composable
private fun ChallengeTimerStrip(
    limitSeconds: Int,
    shownAtMs: Long,
    questionKey: String?,
    frozen: Boolean,
    onTimeout: () -> Unit,
) {
    val kanaColors = LocalKanaColors.current
    var remainingMs by remember(questionKey, limitSeconds) {
        mutableLongStateOf(limitSeconds * 1000L)
    }

    LaunchedEffect(questionKey, limitSeconds, frozen) {
        if (frozen || questionKey == null) return@LaunchedEffect
        val totalMs = limitSeconds * 1000L
        while (isActive) {
            val remaining = totalMs - (System.currentTimeMillis() - shownAtMs)
            remainingMs = remaining.coerceAtLeast(0)
            if (remaining <= 0) {
                onTimeout()
                break
            }
            delay(100)
        }
    }

    val totalMs = limitSeconds * 1000L
    val percent = if (totalMs > 0) (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
    val danger = percent <= 0.25f
    val color = if (danger) kanaColors.wrong else MaterialTheme.colorScheme.secondary

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "剩余时间",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = PracticeSession.formatSeconds(remainingMs / 1000.0),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier.fillMaxWidth(),
            color = color,
        )
    }
}

/** 选择题选项网格，对应 Web 版 .options-grid。 */
@Composable
fun OptionGrid(
    options: List<String>,
    isKanaAnswer: Boolean,
    selectedAnswer: Boolean,
    chosenAnswer: String?,
    correctAnswer: String,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val kanaColors = LocalKanaColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowOptions.forEach { option ->
                    val isCorrectOption = selectedAnswer && option == correctAnswer
                    val isWrongChoice =
                        selectedAnswer && option == chosenAnswer && option != correctAnswer
                    val bg = when {
                        isCorrectOption -> kanaColors.optionCorrectBg
                        isWrongChoice -> kanaColors.optionWrongBg
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val fg = when {
                        isCorrectOption -> kanaColors.right
                        isWrongChoice -> kanaColors.wrong
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isKanaAnswer) 64.dp else 56.dp)
                            .clickable(enabled = !selectedAnswer) { onChoose(option) },
                        shape = MaterialTheme.shapes.medium,
                        color = bg,
                        border = BorderStroke(
                            1.dp,
                            when {
                                isCorrectOption -> kanaColors.right.copy(alpha = 0.5f)
                                isWrongChoice -> kanaColors.wrong.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.outline
                            },
                        ),
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (isKanaAnswer) {
                                KanaText(
                                    text = option,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = fg,
                                    textAlign = TextAlign.Center,
                                )
                            } else {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = fg,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** 输入答题区，对应 Web 版 .input-answer。 */
@Composable
private fun InputAnswerArea(
    enabled: Boolean,
    isKanaAnswer: Boolean,
    selectedAnswer: Boolean,
    lastAnswerCorrect: Boolean?,
    onSubmit: (String) -> Unit,
    questionKey: String,
) {
    val kanaColors = LocalKanaColors.current
    var text by remember(questionKey) { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            placeholder = {
                Text(if (isKanaAnswer) "输入假名" else "输入罗马音")
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = when (lastAnswerCorrect) {
                    true -> kanaColors.right
                    false -> kanaColors.wrong
                    null -> MaterialTheme.colorScheme.primary
                },
            ),
        )
        Button(
            onClick = { onSubmit(text) },
            enabled = enabled && !selectedAnswer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("提交")
        }
    }
}

/** 反馈文案，正确答案部分在假名题下使用假名字体。 */
@Composable
private fun FeedbackText(
    text: String,
    answer: String?,
    style: FeedbackStyle,
    isKanaAnswer: Boolean,
) {
    val kanaColors = LocalKanaColors.current
    val color = when (style) {
        FeedbackStyle.RIGHT -> kanaColors.right
        FeedbackStyle.WRONG -> kanaColors.wrong
        FeedbackStyle.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val kanaFont = LocalKanaFontFamily.current

    Text(
        text = buildAnnotatedString {
            append(text)
            answer?.let {
                withStyle(
                    SpanStyle(
                        fontFamily = if (isKanaAnswer) kanaFont else FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                    ),
                ) {
                    append(it)
                }
            }
        },
        color = color,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** 完成页学习报告，对应 Web 版 renderCompletionReport。 */
@Composable
private fun CompletionReportCard(
    viewModel: KanaTrainerViewModel,
    settings: TrainerSettings,
) {
    val state by viewModel.uiState.collectAsState()
    val report = state.round.report ?: return
    val kanaColors = LocalKanaColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "学习报告",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("${report.accuracy}%", "本轮正确率", Modifier.weight(1f))
                Metric("${report.correctCount}", "答对题数", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("${report.wrongCount}", "答错题数", Modifier.weight(1f))
                Metric(report.averageTime, "平均用时", Modifier.weight(1f))
            }

            Text(
                text = if (report.hasRoundMistakes) {
                    "优先复习下面这些本轮出错的假名。"
                } else {
                    "这一轮没有错题，可以继续扩大范围或切换输入模式巩固。"
                },
                color = if (report.hasRoundMistakes) kanaColors.wrong else kanaColors.right,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )

            ReportSection(title = "本轮错得最多") {
                if (report.hasRoundMistakes) {
                    report.roundMistakes.take(6).forEach { mistake ->
                        KanaMistakeBadge(
                            item = mistake.item,
                            answerScript = mistake.answerScript,
                            settings = settings,
                            misses = mistake.misses,
                        )
                    }
                } else {
                    EmptyNote("本轮没有需要优先复习的错题。")
                }
            }

            ReportSection(title = "薄弱行") {
                if (report.weakRows.isNotEmpty()) {
                    report.weakRows.take(4).forEach { row ->
                        RowBadge(row)
                    }
                } else {
                    EmptyNote("本轮没有明显薄弱行。")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.reviewRoundMistakes() },
                    enabled = report.hasRoundMistakes,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (report.hasRoundMistakes) "再练本轮错题" else "本轮无错题")
                }
                OutlinedButton(
                    onClick = { viewModel.startQuiz() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("重新随机一轮")
                }
                OutlinedButton(
                    onClick = { viewModel.reviewLongTermMistakes() },
                    enabled = report.hasLongTermMistakes,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (report.hasLongTermMistakes) "只练长期薄弱项" else "暂无长期薄弱项")
                }
            }
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

@Composable
private fun KanaMistakeBadge(
    item: KanaItem,
    answerScript: Script?,
    settings: TrainerSettings,
    misses: Int,
) {
    val (primary, secondary) = kanaDisplay(item, answerScript, settings)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KanaText(
                text = primary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KanaText(
                    text = secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = " / ${item.romaji}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "$misses 次错误",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowBadge(row: WeakRow) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${row.misses} 次错误 · ${row.count} 个假名",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
    )
}
