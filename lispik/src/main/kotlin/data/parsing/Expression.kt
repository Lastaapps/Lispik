package data.parsing

import arrow.core.Validated
import arrow.core.andThen
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.FunToken
import domain.model.LToken
import domain.model.Node
import domain.model.tryMatchFun
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

fun Tokens.parseExpression(enableDeFun: Boolean = false): Validated<Error, Node> =
    nextToken().valueOr { return it.invalid() }.let { info ->

        when (val token = info.token) {
            is LToken.Number -> Node.Literal.LInteger(token.value).valid()
            is LToken.Quote -> parseQuote()
            is LToken.Text -> {
                when (val funToken = token.tryMatchFun()) {
                    is FunToken.User ->
                        Node.VariableSubstitution(funToken.name).valid()

                    FunToken.BuiltIn.Nil ->
                        Node.Literal.LNil.valid()

                    else -> Error.ParserError.UnexpectedToken(info).invalid()
                }
            }

            LToken.Bracket.Opened ->
                parseCallable(enableDeFun).andThen { res ->
                    requireToken(LToken.Bracket.Closed).map { res }
                }

            else -> Error.ParserError.UnexpectedToken(info).invalid()
        }
    }

fun Tokens.parseRemainingExpressions(): Validated<Error, ImmutableList<Node>> =
    persistentListOf<Node>().mutate { expressions ->
        while (true) {
            val info = peek().valueOr { return it.invalid() }
            if (info.token == LToken.Bracket.Closed) {
                return@mutate
            }

            expressions += parseExpression().valueOr { return it.invalid() }
        }
    }.valid()
