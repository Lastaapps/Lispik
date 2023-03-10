package data.compiler

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.ByteCode
import domain.model.ByteInstructions
import domain.model.Error
import domain.model.Node

@Suppress("UNUSED_PARAMETER")
fun Node.Nullary.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {

    val inst = when (this) {
        Node.Nullary.Read -> ByteInstructions.Read
    }

    return listOf(inst).flatten().valid()
}

fun Node.Unary.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {
    val c0 = arg0.compileDispatcher(context).valueOr { return it.invalid() }

    val inst = when (this) {
        is Node.Unary.Car -> ByteInstructions.Car
        is Node.Unary.Cdr -> ByteInstructions.Cdr
        is Node.Unary.IsAtom -> ByteInstructions.IsAtom
        is Node.Unary.IsNil -> ByteInstructions.IsNil
        is Node.Unary.IsPair -> ByteInstructions.IsPair
        is Node.Unary.Print -> ByteInstructions.Print
        is Node.Unary.Zero -> {
            return Node.Binary.IsEqual(arg0, Node.Literal.LInteger(0))
                .compile(context)
        }

        is Node.Unary.Not -> {
            return Node.Unary.Zero(arg0)
                .compile(context)
        }
    }

    return listOf(c0, inst).flatten().valid()
}

fun Node.Binary.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {
    val c0 = arg0.compileDispatcher(context).valueOr { return it.invalid() }
    val c1 = arg1.compileDispatcher(context).valueOr { return it.invalid() }

    val inst = when (this) {
        is Node.Binary.Add -> ByteInstructions.MathBinary.Add
        is Node.Binary.Subtract -> ByteInstructions.MathBinary.Sub
        is Node.Binary.Multiply -> ByteInstructions.MathBinary.Mul
        is Node.Binary.Divide -> ByteInstructions.MathBinary.Div
        is Node.Binary.Cons -> ByteInstructions.Cons
        is Node.Binary.Greater -> ByteInstructions.MathBinary.Greater
        is Node.Binary.Lower -> ByteInstructions.MathBinary.Lower
        is Node.Binary.IsEqual -> ByteInstructions.IsEqual
        is Node.Binary.GreaterEqual -> {
            return Node.Unary.Not(
                Node.Binary.Lower(arg0, arg1)
            ).compile(context)
        }

        is Node.Binary.LowerEqual -> {
            return Node.Unary.Not(
                Node.Binary.Greater(arg0, arg1)
            ).compile(context)
        }

        is Node.Binary.And -> {
            return Node.Binary.Multiply(arg0, arg1) // False is 0
                .compile(context)
        }

        is Node.Binary.Or -> {
            return Node.Unary.Not(
                Node.Binary.And(
                    Node.Unary.Not(arg0),
                    Node.Unary.Not(arg1),
                )
            ).compile(context)
        }
    }

    return listOf(c1, c0, inst).flatten().valid()
}

fun Node.Ternary.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {
    val c0 = arg0.compileDispatcher(context).valueOr { return it.invalid() }
    val c1 = arg1.compileDispatcher(context).valueOr { return it.invalid() }
    val c2 = arg2.compileDispatcher(context).valueOr { return it.invalid() }

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

fun Node.Nnary.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {
    val compiled = args.asReversed().map {
        it.compileDispatcher(context).valueOr { arg -> return arg.invalid() }
    }
    return when (this) {
        is Node.Nnary.ListNode -> {
            val consCells = compiled.map { listOf(it, ByteInstructions.Cons) }.flatten()
            (listOf(ByteInstructions.Nil) + consCells).flatten().valid()
        }
    }
}
