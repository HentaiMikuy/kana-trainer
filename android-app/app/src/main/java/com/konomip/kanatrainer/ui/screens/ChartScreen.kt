package com.konomip.kanatrainer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konomip.kanatrainer.data.ChartSection
import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.KanaItem
import com.konomip.kanatrainer.data.Script
import com.konomip.kanatrainer.ui.KanaTrainerViewModel
import com.konomip.kanatrainer.ui.MasteryState
import com.konomip.kanatrainer.ui.components.KanaText
import com.konomip.kanatrainer.ui.theme.LocalKanaColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChartScreen(viewModel: KanaTrainerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val chartScript = state.settings.chartScript
    val sections = remember { KanaData.getChartSections() }
    val records = state.records

    var privacyOn by remember { mutableStateOf(false) }

    // 掌握状态只在记录变化时重新计算一次
    val masteryByItem = remember(records) {
        val map = mutableMapOf<String, MasteryState>()
        KanaData.getAllPracticeItems().forEach { item ->
            map[item.key] = viewModel.getMasteryState(item)
        }
        map
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                TextButton(onClick = { viewModel.goBack() }) { Text("返回") }
            },
            title = {
                Text(text = "五十音速查", fontWeight = FontWeight.Bold)
            },
            actions = {
                TextButton(onClick = { privacyOn = !privacyOn }) {
                    Text(
                        text = if (privacyOn) "显示" else "遮挡",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                TextButton(onClick = { viewModel.toggleChartScript() }) {
                    Text(
                        text = if (chartScript == Script.KATAKANA) "片假名" else "平假名",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(sections.size) { index ->
                ChartSectionBlock(
                    section = sections[index],
                    chartScript = chartScript,
                    masteryByItem = masteryByItem,
                    privacyOn = privacyOn,
                    onSpeak = { viewModel.speakItem(it) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartSectionBlock(
    section: ChartSection,
    chartScript: Script,
    masteryByItem: Map<String, MasteryState>,
    privacyOn: Boolean,
    onSpeak: (KanaItem) -> Unit,
) {
    val summary = remember(section, masteryByItem) {
        buildChartSummary(section, masteryByItem)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = if (privacyOn) {
                    Modifier.alpha(0.12f)
                } else {
                    Modifier
                },
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KanaData.groupChartItemsByRow(section.items).forEach { rowGroup ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KanaText(
                            text = rowGroup.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowGroup.items.forEach { item ->
                                KanaChartCell(
                                    item = item,
                                    chartScript = chartScript,
                                    mastery = masteryByItem[item.key],
                                    privacyOn = privacyOn,
                                    onSpeak = onSpeak,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 汇总 "新 x · 练 x · 弱 x · 稳 x"，对应 Web 版 getChartSectionSummary。 */
private fun buildChartSummary(
    section: ChartSection,
    masteryByItem: Map<String, MasteryState>,
): String {
    val counts = mutableMapOf(
        "new" to 0,
        "practice" to 0,
        "weak" to 0,
        "steady" to 0,
    )
    section.items.forEach { item ->
        masteryByItem[item.key]?.let { counts[it.bucket] = (counts[it.bucket] ?: 0) + 1 }
    }
    val parts = buildList {
        if (counts["new"]!! > 0) add("新 ${counts["new"]}")
        if (counts["practice"]!! > 0) add("练 ${counts["practice"]}")
        if (counts["weak"]!! > 0) add("弱 ${counts["weak"]}")
        if (counts["steady"]!! > 0) add("稳 ${counts["steady"]}")
    }
    return if (parts.isEmpty()) "暂无记录" else parts.joinToString(" · ")
}

@Composable
private fun KanaChartCell(
    item: KanaItem,
    chartScript: Script,
    mastery: MasteryState?,
    privacyOn: Boolean,
    onSpeak: (KanaItem) -> Unit,
) {
    val kanaColors = LocalKanaColors.current
    val kana = if (chartScript == Script.KATAKANA) item.katakana else item.hiragana
    val bucket = mastery?.bucket ?: "new"

    val borderColor = when (bucket) {
        "practice" -> LocalKanaColors.current.let { MaterialTheme.colorScheme.secondary }.copy(alpha = 0.45f)
        "weak" -> kanaColors.wrong.copy(alpha = 0.45f)
        "steady" -> kanaColors.right.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    }
    val bg = when (bucket) {
        "practice" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        "weak" -> kanaColors.optionWrongBg.copy(alpha = 0.7f)
        "steady" -> kanaColors.optionCorrectBg.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surface
    }
    val statusColor = when (bucket) {
        "practice" -> MaterialTheme.colorScheme.secondary
        "weak" -> kanaColors.wrong
        "steady" -> kanaColors.right
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .width(76.dp)
            .clickable(enabled = !privacyOn) { onSpeak(item) },
        shape = MaterialTheme.shapes.small,
        color = bg,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            KanaText(
                text = kana,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                textAlign = TextAlign.Center,
            )
            Text(
                text = item.romaji,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = mastery?.label ?: "新",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = statusColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
