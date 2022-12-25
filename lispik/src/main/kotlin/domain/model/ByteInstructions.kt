package domain.model

import arrow.core.Validated
import arrow.core.andThen
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import data.compiler.compileLiteral
import data.compiler.unwrap
import domain.Parser
import domain.Tokenizer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

inline fun <reified T : ByteCode.Literal> LStack.popTyped(instruction: ByteCode.Instruction): Validated<Error, T> =
    when (val value = pop()) {
        is T -> value.valid()
        else -> Error.ExecutionError.WrongOperandOnStack(instruction, T::class, value::class).invalid()
    }

inline fun <reified T : ByteCode> LCode.popTyped(instruction: ByteCode.Instruction): Validated<Error, T> =
    when (val value = removeFirst()) {
        is T -> value.valid()
        else -> Error.ExecutionError.WrongOperandInByteCode(instruction, T::class, value::class).invalid()
    }

object ByteInstructions {

    sealed interface MathBinary : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 2) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            val arg0 = stack.popTyped<ByteCode.Literal.Integer>(this).valueOr { return it.invalid() }
            val arg1 = stack.popTyped<ByteCode.Literal.Integer>(this).valueOr { return it.invalid() }

            val res = compute(arg0.value, arg1.value).valueOr { return it.invalid() }

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
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 2) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            val arg0 = stack.pop()
            val arg1 = stack.pop()

            stack.push(ByteCode.Literal.LPair(arg0, arg1))

            return Unit.valid()
        }

    }

    data object Car : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            val arg0 = stack.popTyped<ByteCode.Literal.LPair>(this).valueOr { return it.invalid() }

            stack.push(arg0.car)

            return Unit.valid()
        }
    }

    data object Cdr : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
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
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            stack.push(ByteCode.Literal.Nil)
            return Unit.valid()
        }
    }

    /**
     * LDC x – pushes constant x on stack
     */
    data object Ldc : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            val literal = code.popTyped<ByteCode.Literal>(this).valueOr { return it.invalid() }
            stack.push(literal)
            return Unit.valid()
        }
    }

    data object IsEqual : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
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
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            when (stack.pop()) {
                is ByteCode.Literal.Integer -> ByteCode.Literal.False
                is ByteCode.Literal.LPair -> ByteCode.Literal.False
                ByteCode.Literal.Nil -> ByteCode.Literal.True
                is ByteCode.Literal.Closure -> ByteCode.Literal.False
            }.also { stack.push(it) }

            return Unit.valid()
        }
    }

    data object IsAtom : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            when (stack.pop()) {
                is ByteCode.Literal.Integer -> ByteCode.Literal.True
                is ByteCode.Literal.LPair -> ByteCode.Literal.False
                ByteCode.Literal.Nil -> ByteCode.Literal.False
                is ByteCode.Literal.Closure -> ByteCode.Literal.False
            }.also { stack.push(it) }

            return Unit.valid()
        }
    }

    data object IsPair : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            when (stack.pop()) {
                is ByteCode.Literal.Integer -> ByteCode.Literal.False
                is ByteCode.Literal.LPair -> ByteCode.Literal.True
                ByteCode.Literal.Nil -> ByteCode.Literal.False
                is ByteCode.Literal.Closure -> ByteCode.Literal.False
            }.also { stack.push(it) }

            return Unit.valid()
        }
    }

    data object Print : ByteCode.Instruction {

        var testStream: PrintWriter? = null

        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
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

        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {

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

    /**
     * SEL – selects code to execute next based on value on stack
     */
    data object Sel : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            if (stack.size < 1) {
                return Error.ExecutionError.NotEnoughOperandsOnStack(this, 2, stack.size).invalid()
            }

            val arg0 = stack.popTyped<ByteCode.Literal.Integer>(this).valueOr { return it.invalid() }
            val ifTrue = code.popTyped<ByteCode.CodeBlock>(this).valueOr { return it.invalid() }
            val ifFalse = code.popTyped<ByteCode.CodeBlock>(this).valueOr { return it.invalid() }

            val restOfProgram = Dumpable.Code(code.toImmutableList())
            dump.push(restOfProgram)
            code.clear()

            when (arg0) {
                ByteCode.Literal.False ->
                    code.addAll(ifFalse.instructions)

                else ->
                    code.addAll(ifTrue.instructions)
            }

            return Unit.valid()
        }
    }

    /**
     * JOIN – returns to previously saved code (taken from the dump)
     */
    data object Join : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            if (code.isNotEmpty()) {
                return Error.ExecutionError.CodeNotEmptyOnJoin(code.size).invalid()
            }
            if (dump.isEmpty()) {
                return Error.ExecutionError.NothingToTakeFromDump.invalid()
            }

            val toRestore = dump.pop()
            if (toRestore !is Dumpable.Code) {
                return Error.ExecutionError.CannotRestoreOldContext(this).invalid()
            }
            code.addAll(toRestore.code)

            return Unit.valid()
        }
    }

    /**
     * LD (x.y) – loads argument from env and pushes it on stack
     */
    data object Ld : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            val (level, index) = code.popTyped<ByteCode.Literal.LPair>(this).valueOr { return it.invalid() }
            if (level !is ByteCode.Literal.Integer || index !is ByteCode.Literal.Integer) {
                return Error.ExecutionError.InvalidEnvTargetFormat.invalid()
            }

            return if (level == ByteCode.Literal.GlobalContext) {
                env.last()
            } else {
                env[level.value]
            }.let { src ->
                src.getOrElse(index.value) {
                    return Error.ExecutionError.ListWrongFormatOrIndexOfBound.invalid()
                }
            }.let {
                stack.push(it)
            }.valid().void()
        }
    }

    /**
     * LDF - Takes the code and the current env and pushes closure on stack
     */
    data object Ldf : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            val toStore = code.popTyped<ByteCode.CodeBlock>(this).valueOr { return it.invalid() }

            stack.push(ByteCode.Literal.Closure(toStore, env.dropLast(1).toImmutableList()))

            return Unit.valid()
        }
    }

    /**
     * AP – takes closure and list of arguments from stack and applies it
     */
    data object Ap : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            val closure = stack.popTyped<ByteCode.Literal.Closure>(this).valueOr { return it.invalid() }
            val args = stack.popTyped<ByteCode.Literal.LList>(this).valueOr { return it.invalid() }

            val backup = Dumpable.Complete(
                stack.toImmutableList(),
                code.toImmutableList(),
                env.dropLast(1).toPersistentList(),
            )
            dump.push(backup)

            stack.clear()
            code.clear()

            // Empty if global scope is not used or when it is initialized
            if (env.isNotEmpty()) {
                val global = env.last()
                env.clear()
                env.push(global)
            }

            code.addAll(closure.code.instructions)
            env.addAll(closure.env)
            args.toList().tap { env.push(it) }.valueOr { return it.invalid() }

            return Unit.valid()
        }
    }

    /**
     * RTN – returns from a function, returning the last value on stack
     */
    data object Rtn : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            val current = stack.pop()
            val toRestore = dump.pop()
            if (toRestore !is Dumpable.Complete) {
                return Error.ExecutionError.CannotRestoreOldContext(this).invalid()
            }

            stack.addAll(toRestore.stack)
            stack.push(current)

            code.addAll(toRestore.code)

            val global = env.last()
            env.clear()
            env.push(global)
            env.addAll(toRestore.env)

            return Unit.valid()
        }
    }

    /**
     * DUM – adds dummy environment
     */
    data object Dum : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            env.push(persistentListOf())

            return Unit.valid()
        }
    }

    /**
     * RAP – patches dummy env to arg-list and applies the function
     */
    data object Rap : ByteCode.Instruction {
        override fun process(stack: LStack, dump: LDump, code: LCode, env: LEnvironment): Validated<Error, Unit> {
            val closure = stack.popTyped<ByteCode.Literal.Closure>(this).valueOr { return it.invalid() }
            val args = stack.popTyped<ByteCode.Literal.LList>(this).valueOr { return it.invalid() }

            // pop dummy env
            if (env.pop().isNotEmpty()) {
                return Error.ExecutionError.RemovedEnvInsteadOfDummy.invalid()
            }

            val patch = args.toList().map { it.first() }.andThen {
                if (it is ByteCode.Literal.Closure) {
                    it.valid()
                } else {
                    Error.ExecutionError.ClosureRequiredForPathing.invalid()
                }
            }.valueOr { return it.invalid() }
            val patchCode = ByteCode.Literal.Closure(patch.code, persistentListOf())

            val updated = with(closure) {
                copy(
                    env = env.toPersistentList().removeAt(env.lastIndex)
                        .add(persistentListOf(patchCode)),
                )
            }

            val backup = Dumpable.Complete(
                stack.toImmutableList(),
                code.toImmutableList(),
                env.dropLast(1).toPersistentList(),
            )
            dump.push(backup)

            stack.clear()
            code.clear()
            val global = env.last()
            env.clear()
            env.push(global)

            code.addAll(updated.code.instructions)
            env.addAll(updated.env)

            return Unit.valid()
        }
    }
}
