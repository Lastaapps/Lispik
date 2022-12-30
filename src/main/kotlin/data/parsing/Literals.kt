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

fun Tokens.readOneLiteral(requireEnd: Boolean): Validated<Error, Node.Literal> =
    nextToken().andThen { info ->
        when (info.token) {
            LToken.Bracket.Opened -> readLiteralsTillEnd()

            is LToken.Number -> Node.Literal.LInteger(info.token.value).valid()

            is LToken.Text ->
                if (info.token.tryMatchFun() == FunToken.BuiltIn.Nil) {
                    Node.Literal.LNil.valid()
                } else {
                    Error.ParserError.UnexpectedToken(info).invalid()
                }

            else -> Error.ParserError.UnexpectedToken(info).invalid()
        }
    }.andThen { res ->
        if (requireEnd) {
            ensureEnd().map { res }
        } else {
            res.valid()
        }
    }


fun Tokens.readLiteralsTillEnd(): Validated<Error, Node.Literal.LList> =
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
    requireToken(LToken.Bracket.Opened).andThen {
        readLiteralsTillEnd()
    }
