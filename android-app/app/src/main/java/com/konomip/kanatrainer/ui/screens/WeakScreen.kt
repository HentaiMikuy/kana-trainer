package com.konomip.kanatrainer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.konomip.kanatrainer.data.LearningRecords
import com.konomip.kanatrainer.ui.KanaTrainerViewModel
import com.konomip.kanatrainer.ui.components.KanaText
import com.konomip.kanatrainer.ui.theme.LocalKanaColors

/** 容易忘的假名页面，对应 Web 版 weak-panel + reviewMistakesButton + clearRecordsButton。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeakScreen(viewModel: KanaTrainerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    var privacyOn by remember { mutableStateOf(false) }
    var clearConfirmVisible by remember { mutableStateOf(false) }

    val weakList = remember(state.mistakes) { viewModel.getWeakList() }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                TextButton(onClick = { viewModel.goBack() }) { Text("返回") }
            },
            title = {
                Text(text = "容易忘的假名", fontWeight = FontWeight.Bold)
            },
            actions = {
                TextButton(onClick = { privacyOn = !privacyOn }) {
                    Text(
                        text = if (privacyOn) "显示" else "遮挡",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (weakList.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "错题会显示在这里",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(weakList.size) { index ->
                    val record = weakList[index]
                    val item = com.konomip.kanatrainer.data.KanaData.getCanonicalPracticeItem(record)
                    if (item != null) {
                        val (primary, secondary) = kanaDisplay(item, null, state.settings)
                        WeakCard(
                            record = record,
                            primary = primary,
                            secondary = secondary,
                            privacyOn = privacyOn,
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.startMistakeReview(
                                weakList.mapNotNull {
                                    com.konomip.kanatrainer.data.KanaData.getCanonicalPracticeItem(it)
                                },
                            )
                        },
                        enabled = weakList.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("只练错题")
                    }
                    OutlinedButton(
                        onClick = { clearConfirmVisible = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("清空记录", color = LocalKanaColors.current.wrong)
                    }
                }
            }
        }
    }

    if (clearConfirmVisible) {
        AlertDialog(
            onDismissRequest = { clearConfirmVisible = false },
            title = { Text("清空学习记录") },
            text = { Text("确定清空本地学习记录吗？错题和历史正确率都会被移除。") },
            confirmButton = {
                TextButton(onClick = {
                    clearConfirmVisible = false
                    viewModel.clearRecords()
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { clearConfirmVisible = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun WeakCard(
    record: com.konomip.kanatrainer.data.LearningRecord,
    primary: String,
    secondary: String,
    privacyOn: Boolean,
) {
    val kanaColors = LocalKanaColors.current
    val accuracy = LearningRecords.getRecordAccuracy(record)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (privacyOn) 0.12f else 1f),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, kanaColors.wrong.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KanaText(
                text = primary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KanaText(
                        text = secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = " / ${record.romaji}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "${record.misses} 错 · $accuracy%",
                style = MaterialTheme.typography.labelMedium,
                color = kanaColors.wrong,
            )
        }
    }
}
