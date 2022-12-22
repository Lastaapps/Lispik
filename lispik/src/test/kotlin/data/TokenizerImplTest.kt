package data

import data.token.asSequence
import domain.Tokenizer
import domain.model.FunToken
import domain.model.LToken
import domain.model.tryMatchFun
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf


class TokenizerImplTest : FunSpec({

    context("Next token") {

        fun runNextTokenTest(input: String, vararg res: Any) {
            println("Running test for '$input'")

            Tokenizer.from(input)
                .asSequence()
                .toList()
                .map { token -> token.fold({ it }, { it.token }) } shouldBe res.toList()
        }

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
                    LToken.Operator.Add,
                    LToken.Operator.Sub,
                    LToken.Operator.Times,
                    LToken.Operator.Div,
                    LToken.Operator.Lower,
                    LToken.Operator.Greater,
                    LToken.Eof,
            )
        }
        test("Keywords") {

            val input = "pair? eq? atom? null? nil? def defun define"
            println("Running test for '$input'")

            val tokens = Tokenizer.from(input)
                .asSequence()
                .toList()
                .map { token -> token.fold({ it }, { it.token }) }

            listOf(
                FunToken.BuiltIn.IsPair,
                FunToken.BuiltIn.IsEq,
                FunToken.BuiltIn.IsAtom,
                FunToken.BuiltIn.IsNil,
                FunToken.BuiltIn.IsNil,
                FunToken.BuiltIn.DeFun,
                FunToken.BuiltIn.DeFun,
                FunToken.BuiltIn.DeFun,
            ).zip(tokens) { ref, actual ->
                actual should beInstanceOf<LToken.Text>()
                ref shouldBe (actual as LToken.Text).tryMatchFun()
            }
        }
        test("Text and numbers") {
            runNextTokenTest(
                "( ' fun 1234 cons - -1234) if)",
                    LToken.Bracket.Opened,
                    LToken.Quote,
                    LToken.Text("fun"),
                    LToken.Number(1234),
                    LToken.Text("cons"),
                    LToken.Operator.Sub,
                    LToken.Number(-1234),
                    LToken.Bracket.Closed,
                    LToken.Text("if"),
                    LToken.Bracket.Closed,
                    LToken.Eof,
            )
        }
    }
})
