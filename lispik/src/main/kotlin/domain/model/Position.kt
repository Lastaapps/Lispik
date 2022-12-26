package domain.model

data class TokenInfo<out T : LToken>(val token: T, val position: Position)

fun LToken.poss(position: Position) =
    TokenInfo(this, position)

data class Position(val line: Int, val column: Int) {
    override fun toString(): String = "line: $line, column: $column"
}
