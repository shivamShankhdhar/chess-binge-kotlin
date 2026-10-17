package com.example.chessbinge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chessbinge.ui.screens.GameScreen
import com.example.chessbinge.ui.screens.HomeScreen
import com.example.chessbinge.ui.screens.PuzzleScreen
import com.example.chessbinge.ui.theme.BgDark
import com.example.chessbinge.ui.theme.ChessBingeTheme
import com.example.chessbinge.viewmodel.ChessViewModel

enum class Screen {
    HOME,
    GAME,
    PUZZLE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessBingeTheme {
                ChessBingeApp()
            }
        }
    }
}

@Composable
fun ChessBingeApp(
    viewModel: ChessViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    // Intercept back button when inside a match or puzzle
    BackHandler(enabled = currentScreen != Screen.HOME) {
        currentScreen = Screen.HOME
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onStartGame = { currentScreen = Screen.GAME },
                        onStartPuzzle = { currentScreen = Screen.PUZZLE }
                    )
                }

                Screen.GAME -> {
                    GameScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.HOME }
                    )
                }

                Screen.PUZZLE -> {
                    PuzzleScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.HOME }
                    )
                }
            }
        }
    }
}