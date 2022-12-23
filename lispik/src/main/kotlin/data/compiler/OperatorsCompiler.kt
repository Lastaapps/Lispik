package data.compiler

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.ByteCode
import domain.model.ByteInstructions
import domain.model.Error
import domain.model.Node

fun Node.Nullary.compile(): Validated<Error, ByteCode.CodeBlock> {

    val inst = when (this) {
        Node.Nullary.Read -> ByteInstructions.Read
    }

    return listOf(inst).flatten().valid()
}

fun Node.Unary.compile(): Validated<Error, ByteCode.CodeBlock> {
    val c0 = arg0.compile().valueOr { return it.invalid() }

    val inst = when (this) {
        is Node.Unary.Car -> ByteInstructions.Car
        is Node.Unary.Cdr -> ByteInstructions.Cdr
        is Node.Unary.IsAtom -> ByteInstructions.IsAtom
        is Node.Unary.IsNil -> ByteInstructions.IsNil
        is Node.Unary.IsPair -> ByteInstructions.IsPair
        is Node.Unary.Print -> ByteInstructions.Print
    }

    return listOf(c0, inst).flatten().valid()
}

fun Node.Binary.compile(): Validated<Error, ByteCode.CodeBlock> {
    val c0 = arg0.compile().valueOr { return it.invalid() }
    val c1 = arg1.compile().valueOr { return it.invalid() }

    val inst = when (this) {
        is Node.Binary.Add -> ByteInstructions.MathBinary.Add
        is Node.Binary.Subtract -> ByteInstructions.MathBinary.Sub
        is Node.Binary.Multiply -> ByteInstructions.MathBinary.Multiply
        is Node.Binary.Divide -> ByteInstructions.MathBinary.Div
        is Node.Binary.Cons -> ByteInstructions.Cons
        is Node.Binary.Greater -> ByteInstructions.MathBinary.Greater
        is Node.Binary.Lower -> ByteInstructions.MathBinary.Lower
        is Node.Binary.IsEqual -> ByteInstructions.IsEqual
    }

    return listOf(c0, c1, inst).flatten().valid()
}

fun Node.Ternary.compile(): Validated<Error, ByteCode.CodeBlock> {
    val c0 = arg0.compile().valueOr { return it.invalid() }
    val c1 = arg1.compile().valueOr { return it.invalid() }
    val c2 = arg2.compile().valueOr { return it.invalid() }

    when (this) {
        is Node.Ternary.If -> {
            return listOf(
                c0,
                ByteInstructions.Sel,
                ByteCode.CodeBlock(listOf(c1, ByteInstructions.Join).flatten()),
                ByteCode.CodeBlock(listOf(c2, ByteInstructions.Join).flatten()),
            ).flatten().valid()
        }
    }
}
