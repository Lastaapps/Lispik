package data.parsing

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.Parser
import domain.model.Error
import domain.model.LToken
import domain.model.Node
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

typealias Tokens = TokenIterator

data class GlobalScope(
    val functions: ImmutableList<Node.Closures.DeFun>,
    val expressions: ImmutableList<Node>,
)

fun Tokens.parseGlobalScope(): Validated<Error, GlobalScope> {
    val foundFunc = mutableSetOf<String>()
    val functions = mutableListOf<Node.Closures.DeFun>()
    val expressions = mutableListOf<Node>()

    while (true) {
        val info = peek().valueOr { return it.invalid() }
        if (info.token == LToken.Eof) {
            break
        }

        parseExpression(enableDeFun = true).valueOr { return it.invalid() }.let { expression ->
            if (expression is Node.Closures.DeFun) {
                if (foundFunc.contains(expression.name)) {
                    return Error.ParserError.FunctionDefinedTwice(expression.name).invalid()
                }
                foundFunc += expression.name
                functions += expression
            } else {
                expressions += expression
            }
        }
    }

    return GlobalScope(
        functions.toImmutableList(),
        expressions.toImmutableList(),
    ).valid()
}

fun Tokens.parseLiterals(): Validated<Error, ImmutableList<Node.Literal>> =
    persistentListOf<Node.Literal>().mutate { expressions ->
        while (true) {
            val info = peek().valueOr { return it.invalid() }
            if (info.token == LToken.Eof) {
                break
            }

            parseExpression(enableDeFun = true)
                .map {
                    if (it is Node.Literal) {
                        it
                    } else {
                        return Error.ParserError.LiteralsOnly.invalid()
                    }
                }
                .valueOr { return it.invalid() }.let { expression ->
                    expressions += expression
                }
        }
    }.valid()

/**
 * S - starting symbol
 * E - expression
 * X - group of expressions
 * L - literal (int, nil)
 * Q - quoted list
 * C - callable
 * A - arg names
 * N - name
 * O - operator, simple call
 * V - variable
 *
 * S -> ES | e
 * E -> L | Q | (C) | V
 * A -> AN | e
 * X -> EX | e
 * C -> lambda (A) E
 * C -> O X
 * C -> defun (N A) E
 * C -> let (N E) E
 * C -> V X
 * C -> apply V X -> V (list* X)
 */
class ParserImpl(
    private val tokens: Tokens,
) : Parser {
    override fun parseToAST(): Validated<Error, GlobalScope> = tokens.parseGlobalScope()
    override fun parseLiterals(): Validated<Error, ImmutableList<Node.Literal>> = tokens.parseLiterals()
}