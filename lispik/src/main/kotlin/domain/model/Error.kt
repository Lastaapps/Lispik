package domain.model

sealed interface Error {
    sealed interface TokenError : Error {
        val line: Int
        val column: Int

        data class UnknownCharacter(override val line: Int, override val column: Int) : TokenError
        data class NonClosedScope(override val line: Int, override val column: Int) : TokenError
    }
}