package domain

import arrow.core.Validated
import domain.model.Error
import domain.model.LToken
import domain.model.TokenInfo

interface Tokenizer {
    fun nextToken() : Validated<Error.TokenError, TokenInfo<LToken>>
}
