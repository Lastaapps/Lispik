package data

import arrow.core.Either
import arrow.core.Invalid
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Valid
import arrow.core.Validated
import arrow.core.getOrElse
import arrow.core.invalid
import arrow.core.orElse
import arrow.core.some
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.LToken
import util.reduced

internal fun CacheIterator.matchIf(
    move: Boolean = true,
    predicate: (Char) -> Boolean
): Validated<Error.TokenError, Char> =
    current().let { char ->
        Either.conditionally(
            predicate(char),
            { Error.TokenError.UnknownCharacter(line(), position()) },
            { char },
        ).fold({ Invalid(it) }, { Valid(it) })
    }.tap {
        if (move) {
            move()
        }
    }

internal fun CacheIterator.matchChar(char: Char, move: Boolean = true): Validated<Error.TokenError, Char> =
    matchIf(move) { it == char }

internal fun CacheIterator.matchRange(range: CharRange): Validated<Error.TokenError, Char> =
    matchIf { it in range }

//internal fun CacheIterator.matchIterable(coll: Iterable<Char>): Validated<Error.TokenError, Char> =
//    coll.map {
//        { matchChar(it) }
//    }.reduced().invoke()

//internal fun CacheIterator.matchSequenceOf(coll: Iterable<Char>): Validated<Error.TokenError, String> =
//    matchIterable(coll).map { local ->
//        local + matchSequenceOf(coll).valueOr { "" }
//    }

internal fun CacheIterator.matchSequenceOf(range: CharRange): Validated<Error.TokenError, String> =
    matchRange(range).map { local ->
        local + matchSequenceOf(range).valueOr { "" }
    }

/**
 * Ensures, that after a token there is either closing bracket, white space or EOF
 * Does not move iterator forward
 */
internal fun CacheIterator.matchAfterToken() =
    Either.conditionally(
        !hasNext(),
        { Error.TokenError.NonClosedScope(line(), position()) },
        { },
    ).fold({ it.invalid() }, { it.valid() })
        .orElse {
            matchIf(move = false) { it.isWhitespace() }
                .orElse { matchChar(')', move = false) }
        }.mapLeft {
            Error.TokenError.NonClosedScope(line(), position())
        }

internal fun CacheIterator.matchNumber(): Validated<Error.TokenError, Int> =
    matchSequenceOf('0'..'9').map { it.toInt() }

internal fun CacheIterator.matchText(): Validated<Error.TokenError, String> =
    matchSequenceOf('a'..'z').orElse {
        matchSequenceOf('A'..'Z')
    }

internal fun CacheIterator.matchMinusToken() =
    matchChar('-').map {
        when (val res = matchNumber()) {
            is Validated.Valid -> LToken.Number(res.value.unaryMinus())
            is Validated.Invalid -> LToken.Operator.Minus
        }
    }

internal class CacheIterator(
    private val iterator: Iterator<Char>,
) {

    private var current: Option<Char> = None
    private var position = 0
    private var line = 0

    fun hasNext(): Boolean = current is Some

    fun move() {
        if (iterator.hasNext()) {
            iterator.next().also {
                current = it.some()
                position++

                if (it == '\n') {
                    line += 1
                }
            }
        } else {
            current = None
        }
    }

    fun current(): Char = current.orNull()!!

    fun position(): Int = position
    fun line(): Int = line

    init {
        move()
    }
}
