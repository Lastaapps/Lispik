package data.compiler

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.ByteCode
import domain.model.ByteInstructions
import domain.model.Error
import domain.model.Node

fun Node.Named.findCoordinates(context: CompilationContext): Validated<Error, ByteCode.Literal.LPair> {
    context.env.forEachIndexed { envIndex, env ->
        when (val index = env.indexOf(name)) {
            -1 -> return@forEachIndexed
            else -> {
                return ByteCode.Literal.LPair(
                    ByteCode.Literal.Integer(
                        if (envIndex == context.env.lastIndex && context.globalEnabled) {
                            ByteCode.Literal.GlobalContext.value
                        } else {
                            envIndex
                        }
                    ),
                    ByteCode.Literal.Integer(index),
                ).valid()
            }
        }
    }
    return Error.CompilerError.NotFoundByName(name).invalid()
}

fun Node.Call.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> =
    when (this) {
        is Node.Call.ByEvaluation -> compile(context)
        is Node.Call.ByName -> compile(context)
    }

/*
 * (foo 1 2)
 * Nil Ldc 2 Cons Ldc 1 Cons Ldf (0.0) Ap
 * Ldf must return code with Ret inside
 */
private fun Node.Call.ByName.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {
    val coordinate = findCoordinates(context).valueOr { return it.invalid() }

    val arguments = args.asReversed().map { node ->
        node.compileDispatcher(context).valueOr { return it.invalid() }
    }.map {
        listOf(
            it,
            ByteInstructions.Cons,
        ).flatten()
    }
    val call = listOf(ByteInstructions.Ld, coordinate, ByteInstructions.Ap)

    return (listOf(ByteInstructions.Nil) + arguments + call).flatten().valid()
}

private fun Node.Call.ByEvaluation.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {
    val coordinate = toEval.compileDispatcher(context).valueOr { return it.invalid() }

    val arguments = args.asReversed().map { node ->
        node.compileDispatcher(context).valueOr { return it.invalid() }
    }.map {
        listOf(
            it,
            ByteInstructions.Cons,
        ).flatten()
    }
    val call = listOf(coordinate, ByteInstructions.Ap)

    return (listOf(ByteInstructions.Nil) + arguments + call).flatten().valid()
}

fun Node.VariableSubstitution.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {
    val coordinate = findCoordinates(context).valueOr { return it.invalid() }

    return listOf(ByteInstructions.Ld, coordinate).flatten().valid()
}

