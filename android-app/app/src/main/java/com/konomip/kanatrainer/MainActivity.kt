package com.konomip.kanatrainer

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.konomip.kanatrainer.ui.KanaTrainerViewModel
import com.konomip.kanatrainer.ui.Screen
import com.konomip.kanatrainer.ui.screens.ChartScreen
import com.konomip.kanatrainer.ui.screens.GojuonScreen
import com.konomip.kanatrainer.ui.screens.QuizScreen
import com.konomip.kanatrainer.ui.screens.SegmentedRow
import com.konomip.kanatrainer.ui.screens.SettingsScreen
import com.konomip.kanatrainer.ui.screens.WeakScreen
import com.konomip.kanatrainer.ui.theme.KanaTrainerTheme
import com.konomip.kanatrainer.ui.theme.LocalKanaFontFamily

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 系统栏保持透明，内容铺满整屏（状态栏/导航栏图标之下），系统栏图标明暗跟随应用主题，
        // 否则暗色主题下状态栏图标会与背景同色而看不见。
        val darkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(transparent, transparent) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(transparent, transparent) { darkTheme },
        )
        setContent {
            val viewModel: KanaTrainerViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            KanaTrainerTheme(glyphStyle = state.settings.glyphStyle) {
                KanaTrainerApp(viewModel)
            }
        }
    }
}

@Composable
fun KanaTrainerApp(viewModel: KanaTrainerViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = state.screen != Screen.SETTINGS) {
        viewModel.goBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // 安全区全部交给屏幕自己处理（顶部由 TopAppBar 吸收，其余由 edgeToEdgeRoot 让开），
        // 这样状态栏下方不会先空出一段再画 AppBar。Snackbar 仍会自己避开手势条。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        // 两个主页面（记忆练习 / 五十音图）通过各自 TopAppBar 下方的分段控件切换，
        // 水平空间全部留给内容；答题、速查、错题等子流程页面保持沉浸式全屏。
        when (state.screen) {
            Screen.SETTINGS -> SettingsScreen(
                viewModel,
                pageSwitch = { MainNavSwitch(current = state.screen, onSelect = viewModel::navigate) },
            )
            Screen.GOJUON -> GojuonScreen(
                viewModel,
                pageSwitch = { MainNavSwitch(current = state.screen, onSelect = viewModel::navigate) },
            )
            Screen.QUIZ -> QuizScreen(viewModel)
            Screen.CHART -> ChartScreen(viewModel)
            Screen.WEAK -> WeakScreen(viewModel)
        }
    }
}

/** 主页面切换条：记忆练习 / 五十音图，复用练习设置的分段按钮样式，显示在 TopAppBar 下方。 */
@Composable
private fun MainNavSwitch(
    current: Screen,
    onSelect: (Screen) -> Unit,
) {
    SegmentedRow(
        options = listOf(
            Screen.SETTINGS.name to "记忆练习",
            Screen.GOJUON.name to "五十音图",
        ),
        selectedId = current.name,
        onSelect = { onSelect(Screen.valueOf(it)) },
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
    )
}
