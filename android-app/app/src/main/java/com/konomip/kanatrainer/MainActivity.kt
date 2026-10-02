package com.konomip.kanatrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.konomip.kanatrainer.ui.KanaTrainerViewModel
import com.konomip.kanatrainer.ui.Screen
import com.konomip.kanatrainer.ui.screens.ChartScreen
import com.konomip.kanatrainer.ui.screens.QuizScreen
import com.konomip.kanatrainer.ui.screens.SettingsScreen
import com.konomip.kanatrainer.ui.screens.WeakScreen
import com.konomip.kanatrainer.ui.theme.KanaTrainerTheme
import com.konomip.kanatrainer.ui.theme.LocalKanaFontFamily

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (state.screen) {
            Screen.SETTINGS -> SettingsScreen(viewModel, modifier)
            Screen.QUIZ -> QuizScreen(viewModel, modifier)
            Screen.CHART -> ChartScreen(viewModel, modifier)
            Screen.WEAK -> WeakScreen(viewModel, modifier)
        }
    }
}
