package domain

import arrow.core.Validated
import domain.model.Error
import domain.model.LToken

interface Tokenizer {
    fun nextToken() : Validated<Error.TokenError, LToken>
}
