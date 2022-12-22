package data.parsing

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.LToken
import domain.model.Node

fun Tokens.parseApply(): Validated<Error, Node.Apply> {
    val nameToken = nextToken()
        .valueOr { return it.invalid() }

    val args = parseRemainingExpressions().valueOr { return it.invalid() }
    if (args.size == 0) {
        return Error.ParserError.ApplyEmpty.invalid()
    }

    return nameToken.let { info ->
        when (val token = info.token) {
            is LToken.Text -> Node.Apply.ApplyCall(token.name, args).valid()
            is LToken.Operator -> Node.Apply.ApplyOperator(token, args).valid()
            else -> Error.ParserError.NameMissing.invalid()
        }
    }
}
