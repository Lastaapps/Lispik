package domain.model

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import util.MyStack
import java.util.*
import kotlin.collections.ArrayDeque

typealias LStack = Stack<ByteCode.Literal>
typealias LDump = Stack<Dumpable>
typealias LCode = ArrayDeque<ByteCode>
typealias LEnvironment = MyStack<ImmutableList<ByteCode.Literal>>

sealed interface Dumpable {
    data class Complete(
        val stack: ImmutableList<ByteCode.Literal>,
        val code: ImmutableList<ByteCode>,
        val env: ImmutableList<ImmutableList<ByteCode.Literal>>
    ) : Dumpable {
        override fun toString(): String = "(s=$stack, c=$code, e=$env)"
    }

    @JvmInline
    value class Code(
        val code: ImmutableList<ByteCode>,
    ) : Dumpable {
        override fun toString(): String = "(c=$code)"
    }
}

fun ByteCode.protect() = ByteCode.CodeBlock(this)

sealed interface ByteCode {

    @JvmInline
    value class CodeBlock(val instructions: ImmutableList<ByteCode>) : ByteCode {
        constructor(vararg instructions: ByteCode) : this(instructions.toList().toImmutableList())

        override fun toString(): String = "(${instructions.joinToString(separator = ", ")})"
    }

    sealed interface Instruction : ByteCode {
        fun process(
            stack: LStack,
            dump: LDump,
            code: LCode,
            env: LEnvironment,
        ): Validated<Error, Unit>
    }

    sealed interface Literal : ByteCode {
        companion object {
            val True = Integer(1)
            val False = Integer(0)
            val GlobalContext = Integer(-1)
        }

        sealed interface LList : Literal

        @JvmInline
        value class Integer(val value: Int) : Literal {
            override fun toString(): String = value.toString()
        }

        data class LPair(val car: Literal, val cdr: Literal) : Literal, LList {
            override fun toString(): String = "[$car . $cdr]"
        }

        data object Nil : Literal, LList

        sealed interface Closure : Literal {
            val code: CodeBlock
            val env: PersistentList<ImmutableList<Literal>>

            data class Env(override val code: CodeBlock, override val env: PersistentList<ImmutableList<Literal>>) :
                Closure {
                override fun toString(): String = "⟨$code - $env⟩"
            }

            data class Recursive(
                override val code: CodeBlock,
                private val prevEnv: PersistentList<ImmutableList<Literal>>
            ) : Closure {
                override val env: PersistentList<ImmutableList<Literal>>
                    get() = prevEnv.add(0, persistentListOf(this))

                override fun toString(): String = "⟨$code ∞ ${prevEnv}⟩"
            }
        }
    }
}

operator fun ByteCode.Literal.LPair.get(index: Int): Validated<Error, ByteCode.Literal> =
    when {
        index < 0 ->
            Error.ExecutionError.ListWrongFormatOrIndexOfBound.invalid()

        index == 0 -> car.valid()
        cdr !is ByteCode.Literal.LPair ->
            Error.ExecutionError.ListWrongFormatOrIndexOfBound.invalid()

        else -> cdr[index - 1]
    }

fun ByteCode.Literal.LList.toList(
    acu: PersistentList<ByteCode.Literal> = persistentListOf(),
): Validated<Error, ImmutableList<ByteCode.Literal>> =
    when (this) {
        is ByteCode.Literal.LPair -> {
            if (cdr is ByteCode.Literal.LList) {
                cdr.toList(acu.add(car))
            } else {
                Error.ExecutionError.ListWrongFormatOrIndexOfBound.invalid()
            }
        }

        ByteCode.Literal.Nil -> acu.valid()
    }
