package data.parsing

import arrow.core.Validated
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
