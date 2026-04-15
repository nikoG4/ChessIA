package org.nko.chessia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nko.chessia.com.github.krossovochkin.chess.Game
import org.nko.chessia.com.github.krossovochkin.chess.Piece
import org.nko.chessia.com.github.krossovochkin.chess.Square

@Composable
fun ChessCell(
    game: Game,
    square: Square,
    isHighlighted: Boolean,
    isCastleMove: Boolean,
    isPromotionMove: Boolean,
    isKingInCheck: Boolean,
    onClick: () -> Unit
) {
    val row = square.file
    val col = square.rank
    val baseColor = if ((row + col) % 2 == 0) Color.Gray else Color(0xffffad72)
    val backgroundColor = when {
        isKingInCheck -> Color.Red.copy(alpha = 0.7f)
        isCastleMove -> Color.Cyan.copy(alpha = 0.5f) // Enroque resaltado
        isPromotionMove -> Color.Magenta.copy(alpha = 0.5f)
        isHighlighted -> Color.Yellow.copy(alpha = 0.5f)
        else -> baseColor
    }

    val piece = game.board.get(square)

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        piece?.let {
            val pieceSymbol = when (it.type) {
                Piece.Type.Pawn -> "♙"
                Piece.Type.Rook -> "♖"
                Piece.Type.Knight -> "♘"
                Piece.Type.Bishop -> "♗"
                Piece.Type.Queen -> "♕"
                Piece.Type.King -> "♔"
            }
            Text(
                text = pieceSymbol,
                color = if (it.color == Piece.Color.Black) Color.Black else Color.White,
                fontSize = 30.sp
            )
        }
    }
}
