package data.token

import arrow.core.Either
import arrow.core.Invalid
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Valid
import arrow.core.Validated
import arrow.core.getOrElse
import arrow.core.invalid
import arrow.core.some
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.LToken
import domain.model.Position

internal fun CacheIterator.matchIf(
    move: Boolean = true,
    predicate: (Char) -> Boolean
): Validated<Error.TokenError, Char> =
    current()
        .getOrElse {
            return Error.TokenError.EOFReached(position()).invalid()
        }
        .let { char ->
            Either.conditionally(
                predicate(char),
                { Error.TokenError.UnknownCharacter(char, position()) },
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

internal fun CacheIterator.matchSequenceOf(predicate: (Char) -> Boolean): Validated<Error.TokenError, String> =
    matchIf(true, predicate).map { local ->
        local + matchSequenceOf(predicate).valueOr { "" }
    }

///**
// * Ensures, that after a token there is either closing bracket, white space or EOF
// * Does not move iterator forward
// */
//internal fun CacheIterator.matchAfterToken() =
//    Either.conditionally(
//        !hasNext(),
//        { Error.TokenError.NonClosedScope(position()) },
//        { },
//    ).fold({ it.invalid() }, { it.valid() })
//        .orElse {
//            matchIf(move = false) { it.isWhitespace() }
//                .orElse { matchChar(')', move = false) }
//        }.mapLeft {
//            Error.TokenError.NonClosedScope(position())
//        }

internal fun CacheIterator.matchNumber(): Validated<Error.TokenError, Int> =
    matchSequenceOf('0'..'9').map { it.toInt() }

internal fun CacheIterator.matchText(): Validated<Error.TokenError, String> =
    matchSequenceOf {
        it in 'a'..'z' || it in 'A'..'Z' || it in "?_-"
    }

internal fun CacheIterator.matchMinusToken() =
    matchChar('-').map {
        when (val res = matchNumber()) {
            is Validated.Valid -> LToken.Number(res.value.unaryMinus())
            is Validated.Invalid -> LToken.Operator.Sub
        }
    }

internal class CacheIterator(
    private val iterator: Iterator<Char>,
) {

    private var current: Option<Char> = None
    private var line = 0
    private var column = -1
    private var inSingleComment = false
    private var inMultilineComment = false

    fun hasNext(): Boolean = current is Some

    fun move() {
        if (iterator.hasNext()) {
            iterator.next().also {
                current = it.some()
                column++

                if (it == '\n') {
                    line += 1
                    column = 0
                    inSingleComment = false
                }
            }
        } else {
            current = None
        }
    }

    fun current(): Option<Char> = current

    fun position(): Position = Position(line, column)

    init {
        move()
    }

    fun skipWhitespace(): Validated<Error.TokenError, Unit> {
        while (hasNext()) {
            val char = current().getOrElse { break }
            when {
                inMultilineComment && char == '|' -> {
                    move()
                    val next = current().getOrElse {
                        return Error.TokenError.CommentWrongFormat(position()).invalid()
                    }
                    if (next == '#') {
                        inMultilineComment = false
                        move()
                    }
                }

                inSingleComment || inMultilineComment -> move()

                char.isWhitespace() -> move()

                char == ';' -> {
                    inSingleComment = true
                    move()
                }

                !inSingleComment && char == '#' -> {
                    move()
                    val next = current().getOrElse {
                        return Error.TokenError.CommentWrongFormat(position()).invalid()
                    }
                    if (next == '|') {
                        inMultilineComment = true
                        move()
                    } else {
                        Error.TokenError.CommentWrongFormat(position())
                    }
                }

                else -> return Unit.valid()
            }
        }

        return if (inMultilineComment)
            Error.TokenError.UnclosedComment(position()).invalid()
        else Unit.valid()
    }
}
