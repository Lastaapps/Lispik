package domain.model

sealed interface LToken {
    sealed interface Bracket : LToken {
        /** Opening bracket */
        data object Opened : Bracket
        /** Closing bracket */
        data object Closed : Bracket
    }

    data object Quote : LToken

    data class Number(val value: Int) : LToken

    /** User function */
    data class Text(val name: String): LToken

    sealed interface Operator: LToken {
        data object Add : Operator
        data object Sub : Operator
        data object Multiply : Operator
        data object Div : Operator
        data object Greater : Operator
        data object Lower : Operator
    }

    data object Eof : LToken
}
