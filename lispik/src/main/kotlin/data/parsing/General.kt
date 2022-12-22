package data.parsing

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.FunToken
import domain.model.LToken
import domain.model.Node
import domain.model.TokenInfo
import domain.model.tryMatchFun
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf


fun Tokens.parseOperands(): Validated<Error, ImmutableList<Node>> =
    persistentListOf<Node>().mutate { res ->
        while (true) {
            val info = nextToken().valueOr { return it.invalid() }
            when (info.token) {
                LToken.Bracket.Opened -> res += parseListContent()
                    .valueOr { return it.invalid() }

                LToken.Bracket.Closed -> break

                // Literals
                is LToken.Number -> res += Node.Literal.LInteger(info.token.value)
                is LToken.Text -> res +=
                    // TODO handle build-ins like lambdas
                    if (info.token.tryMatchFun() == FunToken.BuiltIn.Nil) {
                        Node.Literal.LNil
                    } else {
                        Node.VariableSubstitution(info.token.name)
                    }

                LToken.Quote -> res += parseQuote().valueOr { return it.invalid() }

                else -> return Error.ParserError.UnexpectedToken(info).invalid()
            }
        }
    }.valid()

fun Tokens.parseListContent(): Validated<Error, Node> {
    val info = nextToken().valueOr { return it.invalid() }

    return when (info.token) {

        is LToken.Operator ->
            @Suppress("UNCHECKED_CAST")
            parseOperator(info as TokenInfo<LToken.Operator>)

        is LToken.Text -> {
            when (val funType = info.token.tryMatchFun()) {
                is FunToken.BuiltIn ->
                    parseBuildIn(funType)

                is FunToken.User ->
                    parseOperands().map { args ->
                        Node.CallByName(funType.name, args)
                    }
            }
        }

        else -> return Error.ParserError.UnexpectedToken(info).invalid()
    }
}
