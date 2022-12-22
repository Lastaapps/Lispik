package data.parsing

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valueOr
import domain.model.Error
import domain.model.FunToken
import domain.model.LToken
import domain.model.Node
import domain.model.TokenInfo
import domain.model.tryMatchFun

fun Tokens.parseCallable(enableDeFun: Boolean = false): Validated<Error, Node> {
    val info = nextToken().valueOr { return it.invalid() }

    return when (info.token) {

        is LToken.Operator ->
            @Suppress("UNCHECKED_CAST")
            parseOperator(info as TokenInfo<LToken.Operator>)

        is LToken.Text -> {
            when (val funType = info.token.tryMatchFun()) {
                is FunToken.BuiltIn ->
                    if (!enableDeFun && funType == FunToken.BuiltIn.DeFun) {
                        return Error.ParserError.DeFunInNonRootScope.invalid()
                    } else {
                        parseBuildIn(funType)
                    }

                is FunToken.User ->
                    parseRemainingExpressions().map { args ->
                        Node.CallByName(funType.name, args)
                    }
            }
        }

        else -> return Error.ParserError.UnexpectedToken(info).invalid()
    }
}