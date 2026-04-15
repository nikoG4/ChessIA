package org.nko.chessia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nko.chessia.com.github.krossovochkin.chess.Piece

@Composable
fun CapturedPieces(capturedPieces: List<Piece>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(4.dp)
    ) {

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val sortedPieces = capturedPieces.sortedBy {
                when (it.type) {
                    Piece.Type.Queen -> 0
                    Piece.Type.Rook -> 1
                    Piece.Type.Bishop -> 2
                    Piece.Type.Knight -> 3
                    Piece.Type.Pawn -> 4
                    Piece.Type.King -> 5
                }
            }

            sortedPieces.forEach { piece ->
                val symbol = when (piece.type) {
                    Piece.Type.Pawn -> "♙"
                    Piece.Type.Rook -> "♖"
                    Piece.Type.Knight -> "♘"
                    Piece.Type.Bishop -> "♗"
                    Piece.Type.Queen -> "♕"
                    Piece.Type.King -> "♔"
                }
                Text(text = symbol, fontSize = 20.sp, modifier = Modifier.padding(1.dp), color = Color.White)
            }
        }
    }
}
