package com.konomip.kanatrainer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konomip.kanatrainer.data.GojuonRow
import com.konomip.kanatrainer.data.GojuonSection
import com.konomip.kanatrainer.data.KanaData
import com.konomip.kanatrainer.data.KanaItem
import com.konomip.kanatrainer.ui.KanaTrainerViewModel
import com.konomip.kanatrainer.ui.MasteryState
import com.konomip.kanatrainer.ui.Screen
import com.konomip.kanatrainer.ui.components.KanaText
import com.konomip.kanatrainer.ui.edgeToEdgeRoot
import com.konomip.kanatrainer.ui.theme.LocalKanaColors
import com.konomip.kanatrainer.ui.theme.KanaTrainerTheme

/** 五十音图页的假名显示方式。 */
private enum class GojuonDisplay(val id: String) {
    BOTH("both"),
    HIRAGANA("hiragana"),
    KATAKANA("katakana");

    companion object {
        fun fromId(id: String): GojuonDisplay = entries.firstOrNull { it.id == id } ?: BOTH
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GojuonScreen(
    viewModel: KanaTrainerViewModel,
    modifier: Modifier = Modifier,
    // 主页面切换控件（分段按钮），由外层注入，显示在 TopAppBar 下方。
    pageSwitch: (@Composable () -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val sections = remember { KanaData.getGojuonSections() }
    val records = state.records

    var display by rememberSaveable { mutableStateOf(GojuonDisplay.BOTH) }

    // 掌握状态只在记录变化时重新计算一次
    val masteryByItem = remember(records) {
        val map = mutableMapOf<String, MasteryState>()
        KanaData.getAllPracticeItems().forEach { item ->
            map[item.key] = viewModel.getMasteryState(item)
        }
        map
    }

    Column(modifier = modifier.edgeToEdgeRoot().fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "GOJŪON CHART",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = "五十音图", fontWeight = FontWeight.Bold)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.navigate(Screen.APP_SETTINGS) }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "设置")
                }
            },
        )

        pageSwitch?.invoke()

        GojuonContent(
            sections = sections,
            display = display,
            onDisplayChange = { display = it },
            masteryByItem = masteryByItem,
            onSpeak = remember(viewModel) { { item -> viewModel.speakItem(item) } },
            modifier = Modifier.weight(1f),
        )
    }
}

/** 一张纵向懒加载表；横向滚动由表外唯一的容器处理，所有行自然同步。 */
@Composable
private fun GojuonContent(
    sections: List<GojuonSection>,
    display: GojuonDisplay,
    onDisplayChange: (GojuonDisplay) -> Unit,
    masteryByItem: Map<String, MasteryState>,
    onSpeak: (KanaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 在记录变化时计算，分区标题重新进入视口不会再次遍历统计。
    val summaries = remember(sections, masteryByItem) {
        sections.associate { it.key to buildGojuonSummary(it, masteryByItem) }
    }
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SegmentedRow(
                options = listOf(
                    GojuonDisplay.BOTH.id to "对照",
                    GojuonDisplay.HIRAGANA.id to "平假名",
                    GojuonDisplay.KATAKANA.id to "片假名",
                ),
                selectedId = display.id,
                onSelect = { onDisplayChange(GojuonDisplay.fromId(it)) },
            )
            Text(
                text = "点击假名听发音 · 新 / 练 / 弱 / 稳表示掌握程度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // 只在视口尺寸或字体比例变化时确定表宽，不在每个列表项内做子组合测量。
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
            val minTableWidth = sections.maxOf { section ->
                (if (section.columns.size == 3) 88.dp else 56.dp) *
                    fontScale * section.columns.size + 6.dp * (section.columns.size - 1)
            } + 24.dp
            val tableWidth = maxOf(maxWidth, minTableWidth)
            val needsHorizontalScroll = tableWidth > maxWidth
            val horizontalState = rememberScrollState()

            Column {
                if (needsHorizontalScroll) {
                    Text(
                        text = "左右滑动查看完整音图 →",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                // 不读取 scrollState.value：滚动只影响放置，不逐帧触发内容重组。
                // 宽度足够时完全省去横向滚动容器。
                val scrollModifier = if (needsHorizontalScroll) {
                    Modifier.horizontalScroll(horizontalState)
                } else {
                    Modifier
                }
                Column(scrollModifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.width(tableWidth).fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        sections.forEach { section ->
                            item(key = "${section.key}:header", contentType = "section-header") {
                                GojuonSectionHeader(section, summaries.getValue(section.key))
                            }
                            // 每行独立组合、测量及回收；离屏的整个分区不再一起创建。
                            // 三列与五列分别复用，减少不同结构之间的重新测量。
                            items(
                                items = section.rows,
                                key = { "${section.key}:row:${it.id}" },
                                contentType = { "kana-row-${section.columns.size}" },
                            ) { row ->
                                GojuonRowContent(row, display, masteryByItem, onSpeak)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GojuonSectionHeader(section: GojuonSection, summary: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GojuonRowContent(
    row: GojuonRow,
    display: GojuonDisplay,
    masteryByItem: Map<String, MasteryState>,
    onSpeak: (KanaItem) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        row.slots.forEach { item ->
            if (item == null) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                GojuonCell(
                    item = item,
                    display = display,
                    mastery = masteryByItem[item.key],
                    onSpeak = onSpeak,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 单元格：对照模式同时显示平假名与片假名，其余模式只显示所选字形；均附罗马音与掌握状态。 */
@Composable
private fun GojuonCell(
    item: KanaItem,
    display: GojuonDisplay,
    mastery: MasteryState?,
    onSpeak: (KanaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val kanaColors = LocalKanaColors.current
    val bucket = mastery?.bucket ?: "new"

    val borderColor = when (bucket) {
        "practice" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
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
        onClick = { onSpeak(item) },
        modifier = modifier.heightIn(min = 88.dp),
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (display) {
                // 对照模式上下显示两种字形，拗音采用更宽的三列布局。
                GojuonDisplay.BOTH -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    KanaText(
                        text = item.hiragana,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, lineHeight = 32.sp),
                        textAlign = TextAlign.Center,
                    )
                    KanaText(
                        text = item.katakana,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                GojuonDisplay.HIRAGANA -> KanaText(
                    text = item.hiragana,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp, lineHeight = 36.sp),
                    textAlign = TextAlign.Center,
                )
                GojuonDisplay.KATAKANA -> KanaText(
                    text = item.katakana,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp, lineHeight = 36.sp),
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = item.romaji,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = mastery?.label ?: "新",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = statusColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** 汇总 "新 x · 练 x · 弱 x · 稳 x"，统计口径与速查表一致。 */
private fun buildGojuonSummary(
    section: GojuonSection,
    masteryByItem: Map<String, MasteryState>,
): String {
    val counts = mutableMapOf(
        "new" to 0,
        "practice" to 0,
        "weak" to 0,
        "steady" to 0,
    )
    section.rows.forEach { row ->
        row.slots.forEach { item ->
            item?.let { kana ->
                masteryByItem[kana.key]?.let { counts[it.bucket] = (counts[it.bucket] ?: 0) + 1 }
            }
        }
    }
    val parts = buildList {
        if (counts["new"]!! > 0) add("新 ${counts["new"]}")
        if (counts["practice"]!! > 0) add("练 ${counts["practice"]}")
        if (counts["weak"]!! > 0) add("弱 ${counts["weak"]}")
        if (counts["steady"]!! > 0) add("稳 ${counts["steady"]}")
    }
    return if (parts.isEmpty()) "暂无记录" else parts.joinToString(" · ")
}


@Preview(name = "Phone · 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Narrow · large text", widthDp = 320, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Composable
private fun GojuonSectionPreview() {
    KanaTrainerTheme {
        GojuonContent(
            sections = remember { KanaData.getGojuonSections() },
            display = GojuonDisplay.BOTH,
            onDisplayChange = {},
            masteryByItem = emptyMap(),
            onSpeak = {},
        )
    }
}

@Preview(name = "Yōon · 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun GojuonYoonPreview() {
    KanaTrainerTheme {
        GojuonContent(
            sections = remember { listOf(KanaData.getGojuonSections().last()) },
            display = GojuonDisplay.BOTH,
            onDisplayChange = {},
            masteryByItem = emptyMap(),
            onSpeak = {},
        )
    }
}
