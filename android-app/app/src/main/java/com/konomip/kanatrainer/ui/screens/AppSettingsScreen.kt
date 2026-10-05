package com.konomip.kanatrainer.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.konomip.kanatrainer.BuildConfig
import com.konomip.kanatrainer.ui.edgeToEdgeRoot
import com.konomip.kanatrainer.ui.theme.KanaTrainerTheme

/** 应用设置，与记忆练习的出题设置分开。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    onOpenUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.edgeToEdgeRoot().fillMaxSize()) {
        TopAppBar(
            title = { Text("设置", fontWeight = FontWeight.Bold) },
            navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Surface(
                    onClick = onOpenUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    ListItem(
                        headlineContent = { Text("应用更新", fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Column {
                                Text("当前版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）")
                                Text("检查新版本 · 下载与安装")
                            }
                        },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "设置 · 浅色")
@Preview(showBackground = true, name = "设置 · 深色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppSettingsPreview() {
    KanaTrainerTheme {
        AppSettingsScreen(onBack = {}, onOpenUpdate = {})
    }
}
