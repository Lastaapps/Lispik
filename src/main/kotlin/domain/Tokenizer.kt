package domain

import arrow.core.Validated
import data.token.TokenizerImpl
import domain.model.Error
import domain.model.LToken
import domain.model.TokenInfo

interface Tokenizer {

    companion object {
        fun from(test: String) = TokenizerImpl(test.iterator())
    }

    fun nextToken(): Validated<Error.TokenError, TokenInfo<LToken>>
}
