package data.compiler

import arrow.core.Validated
import arrow.core.valid
import domain.model.ByteCode
import domain.model.ByteInstructions
import domain.model.Error
import domain.model.Node

fun Node.Literal.compileLiteral(): ByteCode.CodeBlock =
    when (this) {
        is Node.Literal.LInteger -> ByteCode.CodeBlock(
            ByteInstructions.Ldc,
            ByteCode.Literal.Integer(value),
        )

        is Node.Literal.LList -> {
            val content =
                value.asReversed().map { item ->
                    listOf(item.compileLiteral(), ByteInstructions.Cons)
                }.flatten()
            (listOf(ByteInstructions.Nil) + content).flatten()
        }

        Node.Literal.LNil -> ByteCode.CodeBlock(ByteInstructions.Nil)
    }

@Suppress("UNUSED_PARAMETER")
fun Node.Literal.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> =
    compileLiteral().valid()

fun ByteCode.Literal.unwrap(): String =
    when (this) {
        is ByteCode.Literal.Integer -> value.toString()
        is ByteCode.Literal.LPair -> "(" + toList() + ")"
        ByteCode.Literal.Nil -> "nil"
        is ByteCode.Literal.Closure -> "closure"
    }

private fun ByteCode.Literal.LPair.toList(): String {
    val c1 = when (car) {
        is ByteCode.Literal.Integer -> car.value.toString()
        is ByteCode.Literal.LPair -> "(" + car.toList() + ")"
        ByteCode.Literal.Nil -> "nil"
        is ByteCode.Literal.Closure -> "closure"
    }

    val c2 = when (cdr) {
        is ByteCode.Literal.Integer -> ".${cdr.value}"
        is ByteCode.Literal.LPair -> " " + cdr.toList()
        ByteCode.Literal.Nil -> ""
        is ByteCode.Literal.Closure -> ".closure"
    }

    return c1 + c2
}
