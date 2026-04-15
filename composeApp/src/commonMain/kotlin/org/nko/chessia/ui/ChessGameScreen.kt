package org.nko.chessia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import org.nko.chessia.com.github.krossovochkin.chess.Game
import org.nko.chessia.com.github.krossovochkin.chess.Piece
import org.nko.chessia.com.github.krossovochkin.chess.Square
import kotlin.random.Random
import androidx.compose.material.AlertDialog
import org.nko.chessia.models.AIProvider

data class HighlightedSquare(
    val square: Square,
    val isCastle: Boolean = false,
    val isPromotion: Boolean = false
)

@Composable
fun ChessBoardScreen(
    mode: String,
    withTimer: Boolean,
    timerMinutes: Int?,
    difficulty: String?,
    onBack: () -> Unit,
    selectedAI: AIProvider?
) {
    var currentFen by remember { mutableStateOf("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") }

    val playerColor = remember { if (Random.nextBoolean()) Piece.Color.White else Piece.Color.Black }

    var capturedWhitePieces by remember { mutableStateOf<List<Piece>>(emptyList()) }
    var capturedBlackPieces by remember { mutableStateOf<List<Piece>>(emptyList()) }

    val initialTime = (timerMinutes ?: 0) * 60 // en segundos
    var topTime by remember { mutableStateOf(initialTime) }
    var bottomTime by remember { mutableStateOf(initialTime) }

    val isWhiteTurn = remember(currentFen) { currentFen.split(" ")[1] == "w" }

    var gameOver by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<Piece.Color?>(null) }
    var gameOverReason by remember { mutableStateOf("") }


    fun checkGameState(game: Game) {
        if (game.isCheckmate) {
            gameOver = true
            winner = if (game.isWhiteTurn) Piece.Color.Black else Piece.Color.White
            gameOverReason = "Jaque Mate"
        } else if (withTimer) {
            if (topTime <= 0 && playerColor == Piece.Color.White) {
                gameOver = true
                winner = Piece.Color.Black
                gameOverReason = "Tiempo agotado"
            } else if (bottomTime <= 0 && playerColor == Piece.Color.Black) {
                gameOver = true
                winner = Piece.Color.White
                gameOverReason = "Tiempo agotado"
            }
        }
    }

    LaunchedEffect(key1 = isWhiteTurn, key2 = withTimer) {
        if (!withTimer) return@LaunchedEffect
        while (true) {
            delay(1000L)
            if (isWhiteTurn && playerColor != Piece.Color.White) {
                if (topTime > 0) {
                    topTime--
                } else {
                    gameOver = true
                    winner = Piece.Color.White
                    gameOverReason = "Tiempo agotado"
                }
            } else {
                if (bottomTime > 0) {
                    bottomTime--
                } else {
                    gameOver = true
                    winner = Piece.Color.Black
                    gameOverReason = "Tiempo agotado"
                }
            }
        }
    }

    if (gameOver) {
        AlertDialog(
            onDismissRequest = { /* No permitir cerrar haciendo clic fuera */ },
            title = { Text("¡Partida terminada!") },
            text = {
                Column {
                    Text("${gameOverReason} - Ganador: ${winner?.let {
                        if (it == Piece.Color.White) "Blancas" else "Negras"
                    } ?: "Empate"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("¿Qué deseas hacer?")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    gameOver = false
                    winner = null
                    // Reiniciar el juego
                    currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
                    topTime = initialTime
                    bottomTime = initialTime
                    capturedWhitePieces = emptyList()
                    capturedBlackPieces = emptyList()
                }) {
                    Text("Nueva partida")
                }
            },
            dismissButton = {
                TextButton(onClick = onBack) {
                    Text("Volver al menú")
                }
            }
        )
    }

    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return "${if (minutes < 10) "0$minutes" else minutes}:${if (secs < 10) "0$secs" else secs}"
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Modo: $mode", color = Color.LightGray)
            difficulty?.let {
                Text("🎯 Dificultad: $it", color = Color.LightGray)
            }

            if (withTimer) {
                Text(
                    text = "⏱ ${formatTime(topTime)}",
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (playerColor == Piece.Color.White) {
                    CapturedPieces(capturedPieces = capturedBlackPieces)
                } else {
                    CapturedPieces(capturedPieces = capturedWhitePieces)
                }

                Spacer(modifier = Modifier.height(8.dp))

                ChessBoard(
                    fen = currentFen,
                    onFenChange = { newFen ->
                        currentFen = newFen
                        checkGameState(Game.create(newFen)!!)
                    },
                    playerColor = playerColor,
                    onCaptureWhite = { piece -> capturedWhitePieces += piece },
                    onCaptureBlack = { piece -> capturedBlackPieces += piece },
                    selectedAI = selectedAI,
                    difficulty = difficulty
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (playerColor == Piece.Color.White) {
                    CapturedPieces(capturedPieces = capturedWhitePieces)
                } else {
                    CapturedPieces(capturedPieces = capturedBlackPieces)
                }
            }

            if (withTimer) {
                Text(
                    text = "⏱ ${formatTime(bottomTime)}",
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }

            TextButton(onClick = onBack) {
                Text("⬅ Volver al menú", color = Color.LightGray)
            }
        }
    }
}






