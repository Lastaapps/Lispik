package domain

import arrow.core.Validated
import data.parsing.GlobalScope
import data.parsing.ParserImpl
import data.parsing.Tokens
import data.token.asSequence
import domain.model.Error

interface Parser {
    companion object {
        fun from(tokenizer: Tokenizer): Parser =
            ParserImpl(Tokens(tokenizer.asSequence().map { it.toValidated() }.iterator()))
    }

    fun parseToAST(): Validated<Error, GlobalScope>
}