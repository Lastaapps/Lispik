package data.compiler

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import data.parsing.GlobalScope
import domain.Compiler
import domain.model.ByteCode
import domain.model.Error
import domain.model.Node
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Holds function/variable/parameter names
 * The root one is always the global scope
 */
typealias CompilationContext = ImmutableList<ImmutableList<String>>

fun Node.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> =
    when (this) {
        is Node.Literal -> compile(context)

        is Node.Nullary -> compile(context)
        is Node.Unary -> compile(context)
        is Node.Binary -> compile(context)
        is Node.Ternary -> compile(context)

        is Node.Apply -> TODO()
        is Node.Call -> TODO()
        is Node.Closures.DeFun -> TODO()
        is Node.Closures.Lambda -> TODO()
        is Node.Closures.Let -> TODO()
        is Node.Closures.LetRec -> TODO()
        is Node.VariableSubstitution -> TODO()
    }

class CompilerImpl : Compiler {
    override fun compile(scope: GlobalScope): Validated<Error, ByteCode.CodeBlock> {
        return scope.expressions
            .map { expr ->
                val rootContext: CompilationContext = persistentListOf(
                    scope.functions.map { it.name }.toPersistentList()
                )

                expr.compile(rootContext).valueOr { return it.invalid() }
            }
            .flatten()
            .valid()
    }
}