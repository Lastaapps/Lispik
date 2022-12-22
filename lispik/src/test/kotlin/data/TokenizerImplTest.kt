package data

import domain.model.LToken
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe


class TokenizerImplTest : FunSpec({

    fun runNextTokenTest(input: String, vararg res: Any) {
        println("Running test for '$input'")

        TokenizerImpl.from(input)
            .asSequence()
            .toList()
            .map { token -> token.fold({ it }, { it }) } shouldBe res.toList()
    }

    context("Next token") {
        test("Empty") {
            runNextTokenTest(
                "",
                LToken.Eof,
            )
        }
        test("Whitespaces") {
            runNextTokenTest(
                "       ",
                LToken.Eof,
            )
        }
        test("Brackets") {
            runNextTokenTest(
                "()",
                LToken.Bracket.Opened,
                LToken.Bracket.Closed,
                LToken.Eof,
            )
        }
        test("Operators") {
            runNextTokenTest(
                "+ - * / < > ",
                    LToken.Operator.Plus,
                    LToken.Operator.Minus,
                    LToken.Operator.Times,
                    LToken.Operator.Div,
                    LToken.Operator.Lower,
                    LToken.Operator.Greater,
                    LToken.Eof,
            )
        }
        test("Text and numbers") {
            runNextTokenTest(
                "(. ' fun 1234 cons - -1234) if)",
                    LToken.Bracket.Opened,
                    LToken.Dot,
                    LToken.Quote,
                    LToken.Text("fun"),
                    LToken.Number(1234),
                    LToken.Text("cons"),
                    LToken.Operator.Minus,
                    LToken.Number(-1234),
                    LToken.Bracket.Closed,
                    LToken.Text("if"),
                    LToken.Bracket.Closed,
                    LToken.Eof,
            )
        }
    }
})
