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

fun Node.compile(): Validated<Error, ByteCode.CodeBlock> =
    when (this) {
        is Node.Literal -> compile()

        is Node.Nullary -> compile()
        is Node.Unary -> compile()
        is Node.Binary -> compile()
        is Node.Ternary -> compile()

        is Node.Apply -> TODO()
        is Node.CallByName -> TODO()
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
                expr.compile().valueOr { return it.invalid() }
            }
            .flatten()
            .valid()
    }
}