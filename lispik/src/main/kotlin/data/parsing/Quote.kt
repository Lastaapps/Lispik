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
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

private fun Tokens.assertListStart(): Validated<Error, Unit> =
    nextToken().valueOr { return it.invalid() }.let { starting ->
        if (starting.token != LToken.Bracket.Opened)
            return Error.ParserError.UnexpectedToken(starting).invalid()
    }.valid()

private fun Tokens.readLiteralsTillEnd(): Validated<Error, Node.Literal.LList> =
    persistentListOf<Node.Literal>().mutate { res ->
        while (true) {
            val info = nextToken().valueOr { return it.invalid() }
            when (info.token) {
                LToken.Bracket.Opened -> res += readLiteralsTillEnd()
                    .valueOr { return it.invalid() }

                LToken.Bracket.Closed -> break

                is LToken.Number -> res += Node.Literal.LInteger(info.token.value)
                is LToken.Text ->
                    if (info.token.tryMatchFun() == FunToken.BuiltIn.Nil) {
                        res += Node.Literal.LNil
                    }

                else -> return Error.ParserError.UnexpectedToken(info).invalid()
            }
        }
    }.valid().map { Node.Literal.LList(it) }

fun Tokens.parseQuote(): Validated<Error, Node.Literal.LList> =
    assertListStart().andThen {
        readLiteralsTillEnd()
    }
