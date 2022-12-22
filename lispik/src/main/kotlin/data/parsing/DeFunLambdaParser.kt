package data.parsing

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.LToken
import domain.model.Node
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

fun Tokens.parseDefineLambda(withName: Boolean): Validated<Error, Triple<Option<String>, ImmutableList<String>, Node>> {
    requireToken(LToken.Bracket.Opened).tapInvalid { return it.invalid() }

    val name = if (withName) {
        nextToken()
            .valueOr { return it.invalid() }.let { info ->
                if (info.token is LToken.Text) {
                    Some(info.token.name)
                } else {
                    return Error.ParserError.NameMissing.invalid()
                }
            }
    } else {
        None
    }

    val params = persistentListOf<String>().mutate { params ->
        while (true) {
            nextToken().valueOr { return it.invalid() }.let { info ->
                when (info.token) {
                    is LToken.Bracket.Closed -> return@mutate
                    is LToken.Text -> params += info.token.name
                    else -> return Error.ParserError.UnexpectedToken(info).invalid()
                }
            }
        }
    }

    val node = parseExpression().valueOr { return it.invalid() }

    return Triple(name, params, node).valid()
}
