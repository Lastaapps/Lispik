package data.token

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.left
import arrow.core.right
import arrow.core.valid
import arrow.core.valueOr
import domain.Tokenizer
import domain.model.Error
import domain.model.LToken
import domain.model.TokenInfo
import domain.model.poss
import util.reduced

class TokenizerImpl(
    charIterator: Iterator<Char>
) : Tokenizer {
    private val iterator = CacheIterator(charIterator)

    private val matchers = iterator.let {
        // Basic structures (don't require white space after
        listOf(
            '(' to LToken.Bracket.Opened,
            ')' to LToken.Bracket.Closed,
            '\'' to LToken.Quote,
        ).map { (char, token) ->
            { iterator.matchChar(char).map { token } }
        }.let { basic ->

            // Operators
            listOf(
                '+' to LToken.Operator.Add,
                // '-' to LToken.Operator.Minus, - handled in the complex section
                '*' to LToken.Operator.Multiply,
                '/' to LToken.Operator.Div,
                // '<' to LToken.Operator.Lower, // <=
                // '>' to LToken.Operator.Greater, PP >=
            ).map { (char, token) ->
                { iterator.matchChar(char).map { token }/*.requireAfterToken()*/ }
            }.let { simpleMatchers ->
                listOf(
                    { iterator.matchCompareToken(true) },
                    { iterator.matchCompareToken(false) },
                    { iterator.matchMinusToken() },
                    { iterator.matchNumber().map { LToken.Number(it) } },
                    { iterator.matchTextOrDigit().map { LToken.Text(it) } },
                )
                    // .map { { it().requireAfterToken() } }
                    .let { complexMatchers ->
                        (basic + simpleMatchers + complexMatchers).reduced()
                    }
            }
        }
    }

    override fun nextToken(): Validated<Error.TokenError, TokenInfo<LToken>> {
        iterator.skipWhitespace().valueOr { return it.invalid() }

        return iterator.position().let { pos ->
            if (!iterator.hasNext()) {
                LToken.Eof.valid()
            } else {
                matchers()
            }.map { it.poss(pos) }
        }
    }
}

fun Tokenizer.asSequence() = sequence {
    while (true) {
        nextToken().tap { token ->
            yield(token.right())
            if (token.token == LToken.Eof) return@sequence
        }.tapInvalid { token ->
            yield(token.left())
            return@sequence
        }
    }
}
