package data.parsing

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.FunToken
import domain.model.LToken
import domain.model.Node
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

private fun ImmutableList<Node>.requireSize(
    token: FunToken.BuiltIn,
    expected: Int
): Validated<Error, ImmutableList<Node>> =
    if (size == expected) {
        this.valid()
    } else {
        Error.ParserError.InvalidNumberOfArgumentsBuildIn(token, expected, size).invalid()
    }

fun Tokens.requireToken(token: LToken): Validated<Error, Unit> =
    nextToken().valueOr { return it.invalid() }.let { info ->
        if (info.token != token) {
            return Error.ParserError.UnexpectedToken(info).invalid()
        }
    }.valid()

fun Tokens.parseParams(withName: Boolean): Validated<Error, Triple<Option<String>, ImmutableList<String>, Node>> {
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

    requireToken(LToken.Bracket.Opened).tapInvalid { return it.invalid() }

    val node = parseListContent().valueOr { return it.invalid() }

    requireToken(LToken.Bracket.Closed).tapInvalid { return it.invalid() }

    return Triple(name, params, node).valid()
}

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

    val value = parseListContent().valueOr { return it.invalid() }

    val body = parseListContent().valueOr { return it.invalid() }

    requireToken(LToken.Bracket.Closed).tapInvalid { return it.invalid() }

    return Triple(name, value, body).valid()
}

fun Tokens.parseBuildIn(token: FunToken.BuiltIn): Validated<Error, Node> =
    when (token) {
        FunToken.BuiltIn.Cons ->
            parseOperands()
                .valueOr { return it.invalid() }
                .requireSize(token, 2).map {
                    Node.Binary.Cons(it[0], it[1])
                }

        FunToken.BuiltIn.Car ->
            parseOperands()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.Car(it[0])
                }

        FunToken.BuiltIn.Cdr ->
            parseOperands()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.Cdr(it[0])
                }

        FunToken.BuiltIn.Nil -> Node.Literal.LNil.valid()

        FunToken.BuiltIn.If ->
            parseOperands()
                .valueOr { return it.invalid() }
                .requireSize(token, 3).map {
                    Node.Ternary.If(it[0], it[1], it[2])
                }

        FunToken.BuiltIn.IsEq ->
            parseOperands()
                .valueOr { return it.invalid() }
                .requireSize(token, 2).map {
                    Node.Binary.IsEqual(it[0], it[1])
                }

        FunToken.BuiltIn.IsPair ->
            parseOperands()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.IsPair(it[0])
                }

        FunToken.BuiltIn.IsAtom ->
            parseOperands()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.IsAtom(it[0])
                }

        FunToken.BuiltIn.IsNil ->
            parseOperands()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.IsNil(it[0])
                }

        FunToken.BuiltIn.Lambda ->
            parseParams(false).map { (_, params, node) ->
                Node.Closures.Lambda(params, node)
            }

        FunToken.BuiltIn.DeFun ->
            parseParams(true).map { (name, params, node) ->
                Node.Closures.DeFun(name.orNull()!!, params, node)
            }

        FunToken.BuiltIn.Let ->
            parseLet().map { (name, value, body) ->
                Node.Closures.Let(name, value, body)
            }

        FunToken.BuiltIn.LetRec ->
            parseLet().map { (name, value, body) ->
                Node.Closures.LetRec(name, value, body)
            }

        FunToken.BuiltIn.Print ->
            parseOperands()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.Print(it[0])
                }

        FunToken.BuiltIn.Read ->
            parseOperands()
                .valueOr { return it.invalid() }
                .requireSize(token, 0).map {
                    Node.Nullary.Read
                }
    }
