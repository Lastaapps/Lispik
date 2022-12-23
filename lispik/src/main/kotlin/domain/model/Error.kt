package domain.model

import kotlin.reflect.KClass

sealed interface Error {
    sealed interface TokenError : Error {
        val pos: Position

        data class EOFReached(override val pos: Position) : TokenError
        data class UnknownCharacter(override val pos: Position) : TokenError
    }

    sealed interface ParserError : Error {
        data class UnexpectedToken(val token: TokenInfo<LToken>) : ParserError
        data class InvalidNumberOfArgumentsOperator(val token: TokenInfo<LToken>, val expected: Int, val got: Int) :
            ParserError

        data class InvalidNumberOfArgumentsBuildIn(val token: FunToken, val expected: Int, val got: Int) : ParserError
        data object NameMissing : ParserError
        data object ApplyEmpty : ParserError
        data object EndReached : ParserError
        data object DeFunInNonRootScope : ParserError
        data object LiteralsOnly : ParserError
    }

    sealed interface ExecutionError : Error {
        val instruction: ByteCode

        data class NotEnoughOperandsOnStack(
            override val instruction: ByteCode.Instruction,
            val expected: Int,
            val got: Int
        ) : ExecutionError

        data class WrongOperandOnStack(
            override val instruction: ByteCode.Instruction,
            val expected: KClass<out ByteCode.Literal>,
            val got: KClass<out ByteCode.Literal>,
        ) : ExecutionError

        data class WrongOperandInByteCode(
            override val instruction: ByteCode.Instruction,
            val expected: KClass<out ByteCode>,
            val got: KClass<out ByteCode>,
        ) : ExecutionError

        data object DivisionByZero : ExecutionError {
            override val instruction: ByteCode.Instruction
                get() = ByteInstructions.MathBinary.Div
        }

        data class ReadInvalidNumberOfTokens(
            override val instruction: ByteCode.Instruction,
            val expected: Int,
            val got: Int
        ) : ExecutionError

        data class NonInstructionOccurred(override val instruction: ByteCode) : ExecutionError
    }
}