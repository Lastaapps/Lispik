package data.parsing

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.LToken
import domain.model.Position
import domain.model.TokenInfo
import domain.model.poss

private typealias ValidInfo = Validated<Error, TokenInfo<LToken>>

class TokenIterator(
    private val iterator: Iterator<ValidInfo>,
) {
    private var current: ValidInfo = LToken.Eof.poss(Position(-1, -1)).valid()

    fun hasNext(): Boolean = current.valueOr { return false }.token !is LToken.Eof

    fun move() {
        if (iterator.hasNext()) {
            iterator.next().also {
                current = it
            }
        } else {
            current = Error.ParserError.EndReached.invalid()
        }
    }

    fun peek(): ValidInfo = current

    init {
        move()
    }
}

fun TokenIterator.nextToken(): ValidInfo = run {
    peek().also {
        move()
    }
}
