package data.compiler

import arrow.core.Validated
import domain.model.ByteCode
import domain.model.ByteInstructions
import domain.model.Error
import domain.model.Node
import domain.model.protect

fun Node.Closures.DeFun.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {
    val childContext = context.push(params)

    return body.compileDispatcher(childContext).map { child ->
        listOf(child, ByteInstructions.Rtn).flatten()
    }
}

fun Node.Closures.Lambda.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {
    val childContext = context.push(params)

    return body.compileDispatcher(childContext).map { child ->
        listOf(
            ByteInstructions.Ldf,
            listOf(child, ByteInstructions.Rtn).flatten().protect(),
        ).flatten()
    }
}
