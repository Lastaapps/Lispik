package data.parsing

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.LToken
import domain.model.Node

fun Tokens.parseLet(): Validated<Error, Triple<String, Node, Node>> {
    requireToken(LToken.Bracket.Opened).tapInvalid { return it.invalid() }

    val name = nextToken()
        .valueOr { return it.invalid() }
        .let { info ->
            if (info.token is LToken.Text) {
                info.token.name
            } else {
                return Error.ParserError.NameMissing.invalid()
            }
        }

    val value = parseExpression().valueOr { return it.invalid() }

    requireToken(LToken.Bracket.Closed).tapInvalid { return it.invalid() }

    val body = parseExpression().valueOr { return it.invalid() }

    return Triple(name, value, body).valid()
}

