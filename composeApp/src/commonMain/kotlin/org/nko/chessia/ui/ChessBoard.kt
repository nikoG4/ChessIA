package org.nko.chessia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nko.chessia.com.github.krossovochkin.chess.Game
import org.nko.chessia.com.github.krossovochkin.chess.Move
import org.nko.chessia.com.github.krossovochkin.chess.Move.Companion.asMove
import org.nko.chessia.com.github.krossovochkin.chess.Piece
import org.nko.chessia.com.github.krossovochkin.chess.Square
import org.nko.chessia.com.github.krossovochkin.chess.fen.FenSerializer
import org.nko.chessia.models.AIProvider
import org.nko.chessia.services.GameService

@Composable
fun ChessBoard(
    fen: String,
    onFenChange: (String) -> Unit,
    playerColor: Piece.Color,
    onCaptureWhite: (Piece) -> Unit,
    onCaptureBlack: (Piece) -> Unit,
    selectedAI: AIProvider?,
    difficulty: String?
) {
    val game = remember(fen) { Game.create(fen)!! }
    var selectedSquare by remember { mutableStateOf<Square?>(null) }
    var validMoves by remember { mutableStateOf<List<HighlightedSquare>>(emptyList()) }
    var pendingPromotion by remember { mutableStateOf<Pair<Square, Square>?>(null) }

    val isOpponentTurn = (playerColor == Piece.Color.White && !game.isWhiteTurn) ||
            (playerColor == Piece.Color.Black && game.isWhiteTurn)


    LaunchedEffect(isOpponentTurn, fen) {
        if (isOpponentTurn && pendingPromotion == null) {
            val gameService = GameService()
            var retries = 0
            val maxRetries = 3
            var success = false

            while (retries < maxRetries && !success) {
                val opponentMove: String? = withContext(kotlinx.coroutines.Dispatchers.Default) {
                    println("Requesting move from AI (attempt ${retries + 1}/$maxRetries). FEN: $fen")
                    try {
                        gameService.fetchMoveFromAIProvider(fen, difficulty ?: "Media", selectedAI!!)
                    } catch (e: Exception) {
                        println("Error fetching AI move: ${e.message}")
                        null
                    }
                }

                println("Raw AI response: $opponentMove")

                if (opponentMove != null) {
                    // Extract a potential UCI move from the raw response to be robust against extra text
                    val uciMatch = Regex("[a-h][1-8][a-h][1-8][qrbn]?").find(opponentMove.lowercase())?.value
                    if (uciMatch != null) {
                        val move = uciMatch.asMove()
                        if (move != null) {
                            val moved = game.move(move)
                            if (moved) {
                                println("Valid move executed: $uciMatch")
                                success = true
                                onFenChange(FenSerializer.serialize(game.state))
                                selectedSquare = null
                                validMoves = emptyList()
                            } else {
                                println("Engine rejected the move: $uciMatch (Illegal)")
                            }
                        } else {
                            println("Cannot parse matched UCI move: $uciMatch")
                        }
                    } else {
                        println("No UCI format found in response: $opponentMove")
                    }
                }
                
                if (!success) {
                    retries++
                    if (retries < maxRetries) kotlinx.coroutines.delay(1000)
                }
            }

            if (!success) {
                println("Critical: AI failed to provide a valid move after $maxRetries attempts. Entering fallback state (game paused).")
            }
        }
    }


    if (pendingPromotion != null) {
        PromotionDialog(
            onPieceSelected = { selectedPromotion ->
                val (from, to) = pendingPromotion!!
                val piece = game.board.get(from)
                if (piece != null) {
                    val capturedPiece = game.board.get(to)
                    val move = Move.create(piece, from, to, selectedPromotion, isCapture = capturedPiece != null)

                    game.move(move)

                    if (move.isCapture == true && capturedPiece != null) {

                        if (capturedPiece.color == Piece.Color.Black) {
                            onCaptureBlack(capturedPiece)
                        } else {
                            onCaptureWhite(capturedPiece)
                        }
                    }

                    onFenChange(FenSerializer.serialize(game.state))
                }
                selectedSquare = null
                validMoves = emptyList()
                pendingPromotion = null
            },
            onDismiss = { pendingPromotion = null }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val kingInCheckSquare = if (game.isCheck) {
            val kingColor = if (game.isWhiteTurn) Piece.Color.White else Piece.Color.Black
            (0..7).flatMap { file ->
                (0..7).map { rank -> Square(file, rank) }
            }.firstOrNull { sq ->
                game.board.get(sq)?.let { it.type == Piece.Type.King && it.color == kingColor } == true
            }
        } else null

        val rowRange = if (playerColor != Piece.Color.White) 0..7 else 7 downTo 0
        val colRange = if (playerColor != Piece.Color.White) 0..7 else 7 downTo 0

        for (col in colRange) {
            Row {
                for (row in rowRange) {
                    val square = Square(row, col)
                    val highlight = validMoves.find { it.square == square }

                    ChessCell(
                        game = game,
                        square = square,
                        isHighlighted = highlight != null,
                        isCastleMove = highlight?.isCastle == true,
                        isPromotionMove = highlight?.isPromotion == true,
                        isKingInCheck = square == kingInCheckSquare,
                        onClick = {
                            val piece = game.board.get(square)

                            if (selectedSquare != null && validMoves.any { it.square == square }) {
                                val fromPiece = game.board.get(selectedSquare!!)
                                if (fromPiece != null) {
                                    val isPromotionMove = fromPiece.type == Piece.Type.Pawn &&
                                            (square.rank == 0 || square.rank == 7)

                                    if (isPromotionMove) {
                                        pendingPromotion = selectedSquare!! to square
                                    } else {
                                        val capturedPiece = game.board.get(square)
                                        val move = Move.create(fromPiece, selectedSquare!!, square, isCapture = capturedPiece != null)

                                        game.move(move)

                                        if (move.isCapture == true && capturedPiece != null) {
                                            if (capturedPiece.color == Piece.Color.Black) {
                                                onCaptureBlack(capturedPiece)
                                            } else {
                                                onCaptureWhite(capturedPiece)
                                            }
                                        }

                                        onFenChange(FenSerializer.serialize(game.state))
                                        selectedSquare = null
                                        validMoves = emptyList()
                                    }
                                }
                                return@ChessCell
                            }

                            if (piece != null && piece.color == playerColor &&
                                ((game.isWhiteTurn && piece.color == Piece.Color.White) ||
                                        (!game.isWhiteTurn && piece.color == Piece.Color.Black))
                            ) {
                                selectedSquare = square
                                validMoves = game.availableMoves(square).map { move ->
                                    when (move) {
                                        is Move.GeneralMove -> HighlightedSquare(Square(move.toFile, move.toRank))
                                        is Move.PromotionMove -> HighlightedSquare(Square(move.toFile, move.toRank), isPromotion = true)
                                        is Move.CastleMove -> {
                                            val rank = if (game.state.isWhiteTurn) 0 else 7
                                            val file = when (move.type) {
                                                Move.CastleMove.CastleType.Short -> 6
                                                Move.CastleMove.CastleType.Long -> 2
                                            }
                                            HighlightedSquare(Square(file, rank), isCastle = true)
                                        }
                                    }
                                }
                            } else {
                                selectedSquare = null
                                validMoves = emptyList()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PromotionDialog(
    onPieceSelected: (Piece.Type) -> Unit,
    onDismiss: () -> Unit
) {
    val promotionPieces = listOf(
        Piece.Type.Queen,
        Piece.Type.Rook,
        Piece.Type.Bishop,
        Piece.Type.Knight
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Elige una pieza para promocionar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                promotionPieces.forEach { piece ->
                    TextButton(onClick = { onPieceSelected(piece) }) {
                        val symbol = when (piece) {
                            Piece.Type.Queen -> "♕"
                            Piece.Type.Rook -> "♖"
                            Piece.Type.Bishop -> "♗"
                            Piece.Type.Knight -> "♘"
                            else -> "?"
                        }
                        Text(symbol, fontSize = 24.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    }
}
