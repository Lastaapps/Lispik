package data.parsing

import arrow.core.Validated
import arrow.core.andThen
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.LToken
import domain.model.Node
import domain.model.tryMatchFun

fun Tokens.parseApply(): Validated<Error, Node.Apply> {
    val callToken = nextToken()
        .valueOr { return it.invalid() }
    val token = callToken.token

    val args by lazy {
        parseRemainingExpressions()
            .andThen { args ->
                if (args.size == 0) {
                    Error.ParserError.ApplyEmpty.invalid()
                } else args.valid()
            }
    }

    return when (token) {
        LToken.Bracket.Opened -> {
            val node = parseCallable().andThen { node ->
                requireToken(LToken.Bracket.Closed).map { node }
            }.valueOr { return it.invalid() }

            Node.Apply.Eval(node, args.valueOr { return it.invalid() }).valid()
        }

        is LToken.Text -> Node.Apply.Call(token.tryMatchFun(), args.valueOr { return it.invalid() }).valid()
        is LToken.Operator -> Node.Apply.Operator(token, args.valueOr { return it.invalid() }).valid()
        else -> Error.ParserError.ApplyTargetMissingOrInvalid.invalid()
    }
}
