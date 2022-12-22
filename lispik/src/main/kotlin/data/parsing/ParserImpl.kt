package data.parsing

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.Parser
import domain.Tokenizer
import domain.model.Error
import domain.model.Node
import domain.model.LToken
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

typealias Tokens = Tokenizer

data class GlobalScope(
    val functions: Map<String, Node>,
    val expressions: ImmutableList<Node>,
)

fun Tokens.parseGlobalScope(): Validated<Error, GlobalScope> {
    val functions = mutableMapOf<String, Node>()


    val nodes = persistentListOf<Node>().mutate { nodes ->
        while (true) {
            val starting = nextToken().valueOr { return it.invalid() }

            when (starting.token) {
                LToken.Bracket.Opened -> {
                    parseListContent().valueOr { return it.invalid() }.let { node ->
                        if (node is Node.Closures.DeFun) {
                            functions += node.name to node
                        } else {
                            nodes += node
                        }
                    }
                }

                LToken.Eof -> return@mutate
                else -> return Error.ParserError.UnexpectedToken(starting).invalid()
            }
        }
    }

    return GlobalScope(functions, nodes).valid()
}

/**
 * S - starting symbol
 * E - expression
 * X - group of expressions
 * L - literal
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
 * C -> apply V X E -> V (list X E)
 */
class ParserImpl(
    private val tokens: Tokens,
) : Parser {
    override fun parseToAST(): Validated<Error, GlobalScope> = tokens.parseGlobalScope()
}