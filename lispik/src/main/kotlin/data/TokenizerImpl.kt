package data

import arrow.core.Valid
import arrow.core.Validated
import arrow.core.andThen
import arrow.core.left
import arrow.core.right
import domain.Tokenizer
import domain.model.Error
import domain.model.LToken
import util.reduced

class TokenizerImpl(
    charIterator: Iterator<Char>
) : Tokenizer {
    private val iterator = CacheIterator(charIterator)

    companion object {
        fun from(test: String) = TokenizerImpl(test.iterator())
    }

    /**
     * Requires some tokens to be followed by space, closing bracket or EOF
     */
    fun Validated<Error.TokenError, LToken>.requireAfterToken() =
        andThen { token ->
            iterator.matchAfterToken().map { token }
        }

    //TODO require whitespace
    private val matchers = iterator.let {
        // Basic structures (don't require white space after
        listOf(
            '(' to LToken.Bracket.Opened,
            ')' to LToken.Bracket.Closed,
            '.' to LToken.Dot,
            '\'' to LToken.Quote,
        ).map { (char, token) ->
            { iterator.matchChar(char).map { token } }
        }.let { basic ->

            // Operators
            listOf(
                '+' to LToken.Operator.Plus,
                // '-' to LToken.Operator.Minus, - handled in the complex section
                '*' to LToken.Operator.Times,
                '/' to LToken.Operator.Div,
                '<' to LToken.Operator.Lower,
                '>' to LToken.Operator.Greater,
            ).map { (char, token) ->
                { iterator.matchChar(char).map { token }/*.requireAfterToken()*/ }
            }.let { simpleMatchers ->
                listOf(
                    { iterator.matchMinusToken() },
                    { iterator.matchNumber().map { LToken.Number(it) } },
                    { iterator.matchText().map { LToken.Text(it) } },
                )
                    // .map { { it().requireAfterToken() } }
                    .let { complexMatchers ->
                        (basic + simpleMatchers + complexMatchers).reduced()
                    }
            }
        }
    }

    override fun nextToken(): Validated<Error.TokenError, LToken> {
        while (iterator.hasNext() && iterator.current().isWhitespace()) {
            iterator.move()
        }
        if (!iterator.hasNext()) {
            return Valid(LToken.Eof)
        }

        return matchers()
    }
}

fun Tokenizer.asSequence() = sequence {
    while (true) {
        nextToken().let { token ->
            when (token) {
                is Validated.Valid -> {
                    yield(token.value.right())
                    if (token.value == LToken.Eof) return@sequence
                }

                is Validated.Invalid -> {
                    yield(token.value.left())
                    return@sequence
                }
            }
        }
    }
}


