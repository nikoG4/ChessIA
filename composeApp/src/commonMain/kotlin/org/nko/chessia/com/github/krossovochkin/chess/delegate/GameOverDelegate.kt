package org.nko.chessia.com.github.krossovochkin.chess.delegate

import org.nko.chessia.com.github.krossovochkin.chess.GameResult
import org.nko.chessia.com.github.krossovochkin.chess.GameState
import org.nko.chessia.com.github.krossovochkin.chess.fen.FenSerializer
import org.nko.chessia.com.krossovochkin.chess.delegate.InsufficientMaterialDelegate
import org.nko.chessia.com.krossovochkin.chess.delegate.MoveAvailabilityDelegate

internal class GameOverDelegate(
    private val threefoldRepetitionDelegate: ThreefoldRepetitionDelegate,
    private val insufficientMaterialDelegate: InsufficientMaterialDelegate,
    private val halfMoveCountDelegate: HalfMoveCountDelegate,
    private val moveAvailabilityDelegate: MoveAvailabilityDelegate,
    private val attackStateDelegate: AttackStateDelegate,
) {

    fun checkResult(
        state: GameState,
    ): GameResult? {
        if (threefoldRepetitionDelegate.check(FenSerializer.serialize(state))) {
            return GameResult.Draw.ThreefoldRepetition
        }
        if (insufficientMaterialDelegate.check(state.board)) {
            return GameResult.Draw.InsufficientMaterial
        }
        if (halfMoveCountDelegate.check()) {
            return GameResult.Draw.FiftyMoves
        }

        val availableMoves = moveAvailabilityDelegate.getAvailableMoves(state.board, state.currentColor)
        if (availableMoves.isEmpty()) {
            return if (attackStateDelegate.isKingUnderAttack(state.board, state.currentColor)) {
                GameResult.Checkmate(state.currentColor.invert())
            } else {
                GameResult.Draw.Stalemate
            }
        }

        return null
    }
}
