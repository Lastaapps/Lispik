package data.parsing

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.Error
import domain.model.FunToken
import domain.model.Node
import kotlinx.collections.immutable.ImmutableList

private fun ImmutableList<Node>.requireSize(
    token: FunToken.BuiltIn,
    expected: Int
): Validated<Error, ImmutableList<Node>> =
    if (size == expected) {
        this.valid()
    } else {
        Error.ParserError.InvalidNumberOfArgumentsBuildIn(token, expected, size).invalid()
    }


fun Tokens.parseBuildIn(token: FunToken.BuiltIn): Validated<Error, Node> =
    when (token) {
        FunToken.BuiltIn.Cons ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 2).map {
                    Node.Binary.Cons(it[0], it[1])
                }

        FunToken.BuiltIn.Car ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.Car(it[0])
                }

        FunToken.BuiltIn.Cdr ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.Cdr(it[0])
                }

        FunToken.BuiltIn.Nil -> Node.Literal.LNil.valid()

        FunToken.BuiltIn.If ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 3).map {
                    Node.Ternary.If(it[0], it[1], it[2])
                }

        FunToken.BuiltIn.IsEq ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 2).map {
                    Node.Binary.IsEqual(it[0], it[1])
                }

        FunToken.BuiltIn.IsPair ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.IsPair(it[0])
                }

        FunToken.BuiltIn.IsAtom ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.IsAtom(it[0])
                }

        FunToken.BuiltIn.IsNil ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.IsNil(it[0])
                }

        FunToken.BuiltIn.Zero ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.Zero(it[0])
                }

        FunToken.BuiltIn.Not ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.Not(it[0])
                }

        FunToken.BuiltIn.And ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 2).map {
                    Node.Binary.And(it[0], it[1])
                }

        FunToken.BuiltIn.Or ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 2).map {
                    Node.Binary.Or(it[0], it[1])
                }

        FunToken.BuiltIn.Print ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 1).map {
                    Node.Unary.Print(it[0])
                }

        FunToken.BuiltIn.Read ->
            parseRemainingExpressions()
                .valueOr { return it.invalid() }
                .requireSize(token, 0).map {
                    Node.Nullary.Read
                }

        FunToken.BuiltIn.Lambda ->
            parseDefineLambda(false).map { (_, params, node) ->
                Node.Closures.Lambda(params, node)
            }

        FunToken.BuiltIn.DeFun ->
            parseDefineLambda(true).map { (name, params, node) ->
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

        FunToken.BuiltIn.Apply -> parseApply()

        FunToken.BuiltIn.List ->
            parseRemainingExpressions()
                .map { Node.Nnary.ListNode(it) }
    }
