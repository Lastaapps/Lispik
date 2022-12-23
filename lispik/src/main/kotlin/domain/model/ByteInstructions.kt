package domain.model

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import data.compiler.compileLiteral
import data.compiler.unwrap
import domain.Parser
import domain.Tokenizer
import kotlinx.collections.immutable.toImmutableList
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

inline fun <reified T : ByteCode.Literal> LStack.popTyped(instruction: ByteCode.Instruction): Validated<Error, T> =
    when (val value = pop()) {
        is T -> value.valid()
        else -> Error.ExecutionError.WrongOperandOnStack(instruction, T::class, value::class).invalid()
    }

inline fun <reified T : ByteCode> LCodeQueue.popTyped(instruction: ByteCode.Instruction): Validated<Error, T> =
    when (val value = removeFirst()) {
        is T -> value.valid()
        else -> Error.ExecutionError.WrongOperandInByteCode(instruction, T::class, value::class).invalid()
    }

object ByteInstructions {

    sealed interface MathBinary : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 2) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            val arg0 = stack.popTyped<ByteCode.Literal.Integer>(this).valueOr { return it.invalid() }
            val arg1 = stack.popTyped<ByteCode.Literal.Integer>(this).valueOr { return it.invalid() }

            val res = compute(arg1.value, arg0.value).valueOr { return it.invalid() }

            stack.push(ByteCode.Literal.Integer(res))
            return Unit.valid()
        }

        fun compute(arg0: Int, arg1: Int): Validated<Error, Int>

        data object Add : MathBinary {
            override fun compute(arg0: Int, arg1: Int): Validated<Error, Int> = (arg0 + arg1).valid()
        }

        data object Sub : MathBinary {
            override fun compute(arg0: Int, arg1: Int): Validated<Error, Int> = (arg0 - arg1).valid()
        }

        data object Multiply : MathBinary {
            override fun compute(arg0: Int, arg1: Int): Validated<Error, Int> = (arg0 * arg1).valid()
        }

        data object Div : MathBinary {
            override fun compute(arg0: Int, arg1: Int): Validated<Error, Int> =
                if (arg1 != 0) {
                    (arg0 / arg1).valid()
                } else {
                    Error.ExecutionError.DivisionByZero.invalid()
                }
        }

        // TODO support lists
        data object Greater : MathBinary {
            override fun compute(arg0: Int, arg1: Int): Validated<Error, Int> =
                if (arg0 > arg1) {
                    ByteCode.Literal.True.value
                } else {
                    ByteCode.Literal.False.value
                }.valid()
        }

        data object Lower : MathBinary {
            override fun compute(arg0: Int, arg1: Int): Validated<Error, Int> =
                if (arg0 < arg1) {
                    ByteCode.Literal.True.value
                } else {
                    ByteCode.Literal.False.value
                }.valid()
        }
    }

    data object Cons : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 2) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            val arg0 = stack.pop()
            val arg1 = stack.pop()

            stack.push(ByteCode.Literal.LPair(arg1, arg0))

            return Unit.valid()
        }

    }

    data object Car : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            val arg0 = stack.popTyped<ByteCode.Literal.LPair>(this).valueOr { return it.invalid() }

            stack.push(arg0.car)

            return Unit.valid()
        }
    }

    data object Cdr : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            val arg0 = stack.popTyped<ByteCode.Literal.LPair>(this).valueOr { return it.invalid() }

            stack.push(arg0.cdr)

            return Unit.valid()
        }
    }

    @Suppress("unused")
    data object Nil : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            stack.push(ByteCode.Literal.Nil)
            return Unit.valid()
        }
    }

    data object Ldc : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            val literal = code.popTyped<ByteCode.Literal>(this).valueOr { return it.invalid() }
            stack.push(literal)
            return Unit.valid()
        }
    }

    data object IsEqual : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            val arg0 = stack.pop()
            val arg1 = stack.pop()

            stack.push(
                if (arg0 == arg1) {
                    ByteCode.Literal.True
                } else {
                    ByteCode.Literal.False
                }
            )

            return Unit.valid()
        }
    }

    data object IsNil : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            when (stack.pop()) {
                is ByteCode.Literal.Integer -> ByteCode.Literal.False
                is ByteCode.Literal.LPair -> ByteCode.Literal.False
                ByteCode.Literal.Nil -> ByteCode.Literal.True
            }.also { stack.push(it) }

            return Unit.valid()
        }
    }

    data object IsAtom : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            when (stack.pop()) {
                is ByteCode.Literal.Integer -> ByteCode.Literal.True
                is ByteCode.Literal.LPair -> ByteCode.Literal.False
                ByteCode.Literal.Nil -> ByteCode.Literal.False
            }.also { stack.push(it) }

            return Unit.valid()
        }
    }

    data object IsPair : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            when (stack.pop()) {
                is ByteCode.Literal.Integer -> ByteCode.Literal.False
                is ByteCode.Literal.LPair -> ByteCode.Literal.True
                ByteCode.Literal.Nil -> ByteCode.Literal.False
            }.also { stack.push(it) }

            return Unit.valid()
        }
    }

    data object Print : ByteCode.Instruction {

        var testStream: PrintWriter? = null

        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            val literal = stack.peek()
            val unwrapped = literal.unwrap()
            println("[Lispík]# $unwrapped")
            testStream?.println(unwrapped)

            return Unit.valid()
        }
    }

    data object Read : ByteCode.Instruction {

        var stream: BufferedReader = BufferedReader(InputStreamReader(System.`in`))

        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {

            print("[Lispík]> ")
            val line = stream
                .readLine()
                ?.takeIf { it.isNotBlank() }
                ?: run {
                    stack.push(ByteCode.Literal.Nil)
                    return Unit.valid()
                }

            val instructions =
                Parser.from(Tokenizer.from(line))
                    .parseLiterals()
                    .map {
                        if (it.size != 1) {
                            return Error.ExecutionError.ReadInvalidNumberOfTokens(
                                this, 1, it.size,
                            ).invalid()
                        } else it
                    }
                    .map { it.first().compileLiteral() }
                    .valueOr { return it.invalid() }

            stack.push(instructions)

            return Unit.valid()
        }
    }

    data object Sel : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            val arg0 = stack.popTyped<ByteCode.Literal.Integer>(this).valueOr { return it.invalid() }
            val ifTrue = code.popTyped<ByteCode.CodeBlock>(this).valueOr { return it.invalid() }
            val ifFalse = code.popTyped<ByteCode.CodeBlock>(this).valueOr { return it.invalid() }

            val restOfProgram = ByteCode.CodeBlock(code.toImmutableList())
            dump.push(restOfProgram)
            code.clear()

            when (arg0) {
                ByteCode.Literal.True ->
                    code.addAll(ifTrue.instructions)

                ByteCode.Literal.False ->
                    code.addAll(ifFalse.instructions)

                else ->
                    return Error.ExecutionError.IfOnOtherValue(arg0.value).invalid()
            }

            return Unit.valid()
        }
    }

    data object Join : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment): Validated<Error, Unit> {
            if (code.isNotEmpty()) {
                return Error.ExecutionError.CodeNotEmptyOnJoin(code.size).invalid()
            }
            if (dump.isEmpty()) {
                return Error.ExecutionError.NothingToTakeFromDump.invalid()
            }

            val toRestore = dump.pop()
            code.addAll(toRestore.instructions)

            return Unit.valid()
        }
    }
}
