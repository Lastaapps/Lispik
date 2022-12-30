package data.compiler

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.ByteCode
import domain.model.ByteInstructions
import domain.model.Error
import domain.model.FunToken
import domain.model.Node

fun Node.Apply.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> =
    if (args.isEmpty()) {
        Error.CompilerError.ApplyArgsCannotBeEmpty.invalid()
    } else {
        when (this) {
            is Node.Apply.Call -> compile(context)

            is Node.Apply.Operator ->
                Error.CompilerError.ApplyOnBuildInsNotSupported.invalid()

            is Node.Apply.Eval -> compile(context)
        }
    }

private fun Node.Apply.Call.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> =
    when (toCall) {
        is FunToken.BuiltIn ->
            Error.CompilerError.ApplyOnBuildInsNotSupported.invalid()

        is FunToken.User -> {
            val callable = Node.VariableSubstitution(toCall.name).compile(context).valueOr { return it.invalid() }

            val compiled = args.asReversed().map { arg ->
                arg.compileDispatcher(context).valueOr { return it.invalid() }
            }
            val param = compiled.take(1) +
                    compiled.drop(1).map { node -> listOf(node, ByteInstructions.Cons) }.flatten()

            val pushCode = listOf(
                callable,
                ByteInstructions.Ap,
            )

            (param + pushCode).flatten().valid()
        }
    }

private fun Node.Apply.Eval.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {
    val callable = toEval.compileDispatcher(context).valueOr { return it.invalid() }

    val compiled = args.asReversed().map { arg ->
        arg.compileDispatcher(context).valueOr { return it.invalid() }
    }
    val param = compiled.take(1) +
            compiled.drop(1).map { node -> listOf(node, ByteInstructions.Cons) }.flatten()

    val pushCode = listOf(
        callable,
        ByteInstructions.Ap,
    )

    return (param + pushCode).flatten().valid()
}
