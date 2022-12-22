package domain.model

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
    }
}