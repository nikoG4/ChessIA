package org.nko.chessia.com.krossovochkin.chess.delegate

import org.nko.chessia.com.github.krossovochkin.chess.Piece

class MoveCountDelegate(
    currentMove: Int,
) {

    var currentMove: Int = currentMove
        private set

    fun update(color: Piece.Color) {
        if (color == Piece.Color.Black) {
            currentMove++
        }
    }
}
