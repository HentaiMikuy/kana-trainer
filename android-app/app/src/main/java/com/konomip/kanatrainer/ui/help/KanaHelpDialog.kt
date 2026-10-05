package com.konomip.kanatrainer.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class HelpEntry(val title: String, val body: String)

private val HELP_ENTRIES = listOf(
    HelpEntry(
        "侧边导航",
        "左侧导航栏可在「记忆练习」和「五十音图」两个主页面之间切换，练习进度互不影响。",
    ),
    HelpEntry(
        "五十音图",
        "完整展示清音、浊音、半浊音和拗音的经典表格，支持对照 / 平假名 / 片假名三种显示方式，点击假名可播放发音，也可用「遮挡」模糊内容进行自测。",
    ),
    HelpEntry(
        "练习设置",
        "选择片假名、平假名或混合练习，调整答题方向、答题方式、题量、练习范围和限时挑战。",
    ),
    HelpEntry(
        "答题卡",
        "展示当前题目、进度、答案选项或输入框。可以发音、查看答案，并进入下一题。",
    ),
    HelpEntry(
        "五十音速查",
        "快速查看假名、罗马音和掌握状态。练习时可用遮挡按钮模糊内容，避免提前看到答案。",
    ),
    HelpEntry(
        "容易忘的假名",
        "根据答题记录列出容易出错的假名，可一键进入错题练习，也可以清空历史记录。",
    ),
    HelpEntry(
        "统计与数据",
        "顶部统计当前轮次的已答、正确率、连对和平均用时；学习数据支持 JSON 导出和导入，可与 Web 版互通。",
    ),
)

/** 页面说明弹窗，内容与 Web 版 helpDialog 一致。 */
@Composable
fun KanaHelpDialog(visible: Boolean, onDismiss: () -> Unit) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("页面说明", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("这个页面用于练习五十音的识别、罗马音对应和易混淆假名记忆。")
                HELP_ENTRIES.forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = entry.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
    )
}
