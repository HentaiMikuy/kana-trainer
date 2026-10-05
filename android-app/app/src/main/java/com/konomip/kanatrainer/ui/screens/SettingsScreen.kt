package com.konomip.kanatrainer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.LearningRecords
import com.konomip.kanatrainer.logic.AnswerMode
import com.konomip.kanatrainer.logic.CharMode
import com.konomip.kanatrainer.logic.Direction
import com.konomip.kanatrainer.logic.GlyphStyle
import com.konomip.kanatrainer.logic.PracticeType
import com.konomip.kanatrainer.logic.PronunciationMode
import com.konomip.kanatrainer.ui.KanaTrainerViewModel
import com.konomip.kanatrainer.ui.components.ControlGroup
import com.konomip.kanatrainer.ui.edgeToEdgeRoot
import com.konomip.kanatrainer.ui.components.KanaText
import com.konomip.kanatrainer.ui.components.SliderRow
import com.konomip.kanatrainer.ui.components.StatStrip
import com.konomip.kanatrainer.ui.components.SwitchRow
import com.konomip.kanatrainer.ui.help.KanaHelpDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: KanaTrainerViewModel,
    modifier: Modifier = Modifier,
    // 主页面切换控件（分段按钮），由外层注入，显示在 TopAppBar 下方。
    pageSwitch: (@Composable () -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> viewModel.exportRecords(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> viewModel.importRecords(uri) }

    Column(modifier = modifier.edgeToEdgeRoot().fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "KANA TRAINER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = "五十音记忆测试", fontWeight = FontWeight.Bold)
                }
            },
            actions = {
                TextButton(onClick = { viewModel.openHelp() }) {
                    Text("?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
        )

        pageSwitch?.invoke()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                StatStrip(
                    answered = state.round.answered,
                    accuracy = state.round.accuracy,
                    streak = state.round.streak,
                    averageTime = state.round.averageTimeText,
                )
            }

            // 快速入口：速查表与错题本
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NavCard(
                        title = "五十音速查",
                        subtitle = "掌握状态 · 点击发音",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigate(com.konomip.kanatrainer.ui.Screen.CHART) },
                    )
                    NavCard(
                        title = "容易忘的假名",
                        subtitle = "${state.mistakes.size} 个长期薄弱项",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigate(com.konomip.kanatrainer.ui.Screen.WEAK) },
                    )
                }
            }

            item {
                ControlGroup(label = "练习类型") {
                    SegmentedRow(
                        options = listOf(
                            PracticeType.NORMAL.id to "普通",
                            PracticeType.CONFUSING.id to "易混淆",
                        ),
                        selectedId = settings.practiceType.id,
                        onSelect = { viewModel.setPracticeType(PracticeType.fromId(it)) },
                    )
                }
            }

            if (settings.practiceType == PracticeType.CONFUSING) {
                item {
                    ControlGroup(label = "专项分组") {
                        ConfusingChips(
                            selectedId = settings.selectedConfusingSet,
                            onSelect = { viewModel.setConfusingSet(it) },
                        )
                    }
                }
            }

            if (settings.practiceType == PracticeType.NORMAL) {
                item {
                    ControlGroup(label = "测试字符") {
                        SegmentedRow(
                            options = listOf(
                                CharMode.KATAKANA.id to "片假名",
                                CharMode.HIRAGANA.id to "平假名",
                                CharMode.MIXED.id to "混合",
                            ),
                            selectedId = settings.mode.id,
                            onSelect = { viewModel.setMode(CharMode.fromId(it)) },
                        )
                    }
                }
                item {
                    ControlGroup(label = "练习范围") {
                        RowChips(
                            selectedRows = settings.selectedRows,
                            onToggle = { viewModel.toggleRow(it) },
                        )
                    }
                }
                item {
                    SwitchRow(
                        title = "包含浊音 / 半浊音",
                        subtitle = "ガ、ザ、パ 等",
                        checked = settings.includeDakuten,
                        onCheckedChange = { viewModel.setIncludeDakuten(it) },
                    )
                }
                item {
                    SwitchRow(
                        title = "包含拗音",
                        subtitle = "キャ、ショ、ピョ 等",
                        checked = settings.includeSmallKana,
                        onCheckedChange = { viewModel.setIncludeSmallKana(it) },
                    )
                }
            }

            item {
                ControlGroup(label = "测试方向") {
                    SegmentedRow(
                        options = listOf(
                            Direction.KANA_TO_ROMAJI.id to "假名 → 罗马音",
                            Direction.ROMAJI_TO_KANA.id to "罗马音 → 假名",
                        ),
                        selectedId = settings.direction.id,
                        onSelect = { viewModel.setDirection(Direction.fromId(it)) },
                    )
                }
            }

            item {
                ControlGroup(label = "答题方式") {
                    SegmentedRow(
                        options = listOf(
                            AnswerMode.CHOICE.id to "选择",
                            AnswerMode.INPUT.id to "输入",
                        ),
                        selectedId = settings.answerMode.id,
                        onSelect = { viewModel.setAnswerMode(AnswerMode.fromId(it)) },
                    )
                }
            }

            item {
                ControlGroup(label = "字形显示") {
                    SegmentedRow(
                        options = listOf(
                            GlyphStyle.PRINT.id to "印刷体",
                            GlyphStyle.HAND.id to "手写体",
                        ),
                        selectedId = settings.glyphStyle.id,
                        onSelect = { viewModel.setGlyphStyle(GlyphStyle.fromId(it)) },
                    )
                }
            }

            item {
                ControlGroup(label = "发音方式") {
                    SegmentedRow(
                        options = listOf(
                            PronunciationMode.MANUAL.id to "手动",
                            PronunciationMode.AUTO.id to "自动",
                        ),
                        selectedId = settings.pronunciationMode.id,
                        onSelect = { viewModel.setPronunciationMode(PronunciationMode.fromId(it)) },
                    )
                }
            }

            item {
                SwitchRow(
                    title = "限时挑战",
                    subtitle = "超时自动判错并记录平均用时",
                    checked = settings.challengeEnabled,
                    onCheckedChange = { viewModel.setChallengeEnabled(it) },
                )
            }

            if (settings.challengeEnabled) {
                item {
                    SliderRow(
                        label = "每题限时",
                        value = settings.challengeLimitSeconds,
                        valueRange = 5f..30f,
                        step = 4,
                        valueText = "${settings.challengeLimitSeconds} 秒",
                        minLabel = "5 秒",
                        maxLabel = "30 秒",
                        onValueChange = { viewModel.setChallengeLimitSeconds(it) },
                    )
                }
            }

            item {
                SliderRow(
                    label = "题量",
                    value = settings.configuredQuestionLimit,
                    valueRange = 10f..50f,
                    step = 7,
                    valueText = "${settings.configuredQuestionLimit} 题",
                    minLabel = "10",
                    maxLabel = "50",
                    onValueChange = { viewModel.setConfiguredQuestionLimit(it) },
                )
            }

            item {
                ControlGroup(label = "学习数据") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                exportLauncher.launch(
                                    com.konomip.kanatrainer.data.LearningRecords.getLearningExportFilename(),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("导出")
                        }
                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(
                                    arrayOf("application/json", "text/plain", "application/octet-stream"),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("导入")
                        }
                    }
                    Text(
                        text = "导出为 JSON，导入会合并并覆盖同一假名的历史记录。与 Web 版导出文件互通。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Button(
                    onClick = { viewModel.startQuiz() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "开始新一轮",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
        }
    }

    state.pendingImport?.let {
        AlertDialog(
            onDismissRequest = { viewModel.dismissImport() },
            title = { Text("导入学习数据") },
            text = { Text("导入学习数据会合并并覆盖同一假名的历史记录，是否继续？") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport() }) { Text("继续导入") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImport() }) { Text("取消") }
            },
        )
    }

    KanaHelpDialog(visible = state.helpVisible, onDismiss = { viewModel.closeHelp() })
}

/** 分段按钮行，对应 Web 版 .segmented。 */
@Composable
fun SegmentedRow(
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, label) ->
            val selected = id == selectedId
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(id) },
                shape = MaterialTheme.shapes.medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                tonalElevation = if (selected) 0.dp else 1.dp,
                border = BorderStroke(
                    1.dp,
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 五十音行选择 chips，对应 Web 版 .row-picker。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowChips(
    selectedRows: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KanaData.BASE_ROWS.forEach { row ->
            Chip(
                label = row.label,
                selected = selectedRows.contains(row.id),
                isKana = true,
                onClick = { onToggle(row.id) },
            )
        }
    }
}

/** 易混淆专项分组 chips，对应 Web 版 .confusing-picker。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfusingChips(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip(label = "全部", selected = selectedId == "all", isKana = false, onClick = {
            onSelect("all")
        })
        KanaData.CONFUSING_KANA_SETS.forEach { set ->
            Chip(label = set.label, selected = selectedId == set.id, isKana = true, onClick = {
                onSelect(set.id)
            })
        }
    }
}

@Composable
fun Chip(
    label: String,
    selected: Boolean,
    isKana: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (selected) 0.dp else 1.dp,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        if (isKana) {
            KanaText(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        } else {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun NavCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
