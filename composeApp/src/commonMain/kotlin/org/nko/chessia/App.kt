package org.nko.chessia

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.nko.chessia.models.AIProvider
import org.nko.chessia.services.GameService
import org.nko.chessia.ui.ChessBoardScreen
import org.nko.chessia.ui.GameModeSelector
import org.nko.chessia.ui.GameSettingsScreen

@Composable
@Preview
fun App() {
    MaterialTheme {

        var currentScreen by remember { mutableStateOf("selector") }
        var selectedMode by remember { mutableStateOf("") }
        var selectedAI by remember { mutableStateOf<AIProvider?>(null) }
        var withTimer by remember { mutableStateOf(false) }
        var timerMinutes by remember { mutableStateOf(5) }
        var difficulty by remember { mutableStateOf<String?>(null) }
        var roomId by remember { mutableStateOf<String?>(null) }

        when (currentScreen) {
            "selector" -> GameModeSelector(
                onModeSelected = {
                    selectedMode = it
                    currentScreen = "settings"
                },
                onSettingsClick = {
                    selectedMode = "Configuración de IA"
                    currentScreen = "settings"
                }
            )

            "settings" -> GameSettingsScreen(
                selectedMode = selectedMode,
                onBack = { currentScreen = "selector" },
                onStartGame = { withTime, time, diff, sAI, room ->
                    selectedAI = sAI
                    withTimer = withTime
                    timerMinutes = time
                    difficulty = if (selectedMode.contains("🧑 Player vs 🧑 Player")) null else diff
                    roomId = room
                    currentScreen = "board"
                }
            )

            "board" -> ChessBoardScreen(
                mode = selectedMode,
                withTimer = withTimer,
                timerMinutes = timerMinutes,
                difficulty = difficulty,
                selectedAI = selectedAI,
                roomId = roomId,
                onBack = {
                    currentScreen = "selector"
                }
            )
        }
    }
}
