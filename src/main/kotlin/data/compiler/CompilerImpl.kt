package data.compiler

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import data.parsing.GlobalScope
import domain.Compiler
import domain.model.ByteCode
import domain.model.ByteInstructions
import domain.model.Error
import domain.model.Node
import domain.model.protect
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Holds function/variable/parameter names
 * The root one is always the global scope
 */
data class CompilationContext(
    val env: PersistentList<ImmutableList<String>>,
    val globalEnabled: Boolean,
)

fun CompilationContext.push(env: ImmutableList<String>) =
    copy(env = this.env.add(0, env))

fun Node.compileDispatcher(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> =
    when (this) {
        is Node.Literal -> compile(context)

        is Node.Nullary -> compile(context)
        is Node.Unary -> compile(context)
        is Node.Binary -> compile(context)
        is Node.Ternary -> compile(context)
        is Node.Nnary -> compile(context)

        is Node.Apply -> compile(context)
        is Node.Call -> compile(context)
        is Node.Closures.DeFun -> compile(context)
        is Node.Closures.Lambda -> compile(context)
        is Node.Closures.Let -> compile(context)
        is Node.Closures.LetRec -> compile(context)
        is Node.VariableSubstitution -> compile(context)
    }

class CompilerImpl : Compiler {

    override fun compile(scope: GlobalScope, createGlobalEnv: Boolean): Validated<Error, ByteCode.CodeBlock> {
        val rootContext = CompilationContext(
            persistentListOf(
                scope.functions.map { it.name }.toPersistentList(),
            ),
            createGlobalEnv,
        )

        if (!createGlobalEnv && scope.functions.isNotEmpty()) {
            return Error.CompilerError.FunctionsUsedWithoutGlobalEnv.invalid()
        }

        // creating global env
        val globalEnv = scope.functions.map { func ->
            func.compileDispatcher(rootContext).valueOr { return it.invalid() }
        }.reversed()
            .map { listOf(ByteInstructions.Ldf, it.protect(), ByteInstructions.Cons) }
            .flatten()
            .let { listOf(ByteInstructions.Nil) + it }
            .flatten()

        val expressions = scope.expressions
            .map { expr ->
                expr.compileDispatcher(rootContext).valueOr { return it.invalid() }
            }
            .flatten()

        return if (createGlobalEnv) {
            listOf(
                globalEnv,
                ByteInstructions.Ldf,
                expressions.protect(),
                ByteInstructions.Ap,
            ).flatten()
        } else {
            expressions
        }.valid()
    }
}