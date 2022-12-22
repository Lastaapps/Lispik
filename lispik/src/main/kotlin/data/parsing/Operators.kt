package data.parsing

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.LToken
import domain.model.Node
import domain.model.TokenInfo

fun Tokens.parseOperator(info: TokenInfo<LToken.Operator>): Validated<Error, Node> =
    parseRemainingExpressions()
        .valueOr { return it.invalid() }
        .let { args ->
            if (args.size != 2) {
                return Error.ParserError.InvalidNumberOfArgumentsOperator(info, 2, args.size).invalid()
            }
            args.let { (a0, a1) ->
                when (info.token) {
                    LToken.Operator.Add -> Node.Binary.Add(a0, a1)
                    LToken.Operator.Sub -> Node.Binary.Subtract(a0, a1)
                    LToken.Operator.Times -> Node.Binary.Multiply(a0, a1)
                    LToken.Operator.Div -> Node.Binary.Divide(a0, a1)
                    LToken.Operator.Lower -> Node.Binary.Lower(a0, a1)
                    LToken.Operator.Greater -> Node.Binary.Greater(a0, a1)
                }
            }.valid()
        }
