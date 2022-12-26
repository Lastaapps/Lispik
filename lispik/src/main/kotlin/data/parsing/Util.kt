package data.parsing

import arrow.core.Validated
import arrow.core.andThen
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.LToken


fun Tokens.requireToken(token: LToken): Validated<Error, Unit> =
    nextToken().valueOr { return it.invalid() }.let { info ->
        if (info.token != token) {
            return Error.ParserError.UnexpectedToken(info).invalid()
        }
    }.valid()

fun Tokens.ensureEnd(): Validated<Error, Unit> =
    nextToken().andThen {
        if (it.token is LToken.Eof) {
            Unit.invalid()
        } else {
            Error.ParserError.OnlyOneExpressionAllowed.valid()
        }
    }.swap().void()
