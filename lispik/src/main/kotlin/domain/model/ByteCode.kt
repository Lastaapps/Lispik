package domain.model

import arrow.core.Validated
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.util.*
import kotlin.collections.ArrayDeque

typealias LStack = Stack<ByteCode.Literal>
typealias LDump = Stack<ByteCode.CodeBlock>
typealias LCodeQueue = ArrayDeque<ByteCode>
typealias LEnvironment = Unit

sealed interface ByteCode {

    data class CodeBlock(val instructions: ImmutableList<ByteCode>) : ByteCode {
        constructor(vararg instructions: ByteCode) : this(instructions.toList().toImmutableList())
    }

    sealed interface Instruction : ByteCode {
        fun process(
            stack: LStack,
            dump: LDump,
            code: LCodeQueue,
            env: LEnvironment,
        ): Validated<Error, Unit>
    }

    sealed interface Literal : ByteCode {
        companion object {
            val True = Integer(1)
            val False = Integer(0)
        }

        data class Integer(val value: Int) : Literal

        data class LPair(val car: Literal, val cdr: Literal) : Literal
        data object Nil : Literal
    }
}