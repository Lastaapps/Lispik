package data

import data.token.asSequence
import domain.Tokenizer
import domain.model.Error
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

        fun failTokenTest(input: String) {
            println("Running test for '$input'")

            Tokenizer.from(input)
                .asSequence()
                .toList()
                .map { token -> token.fold({ it }, { it.token }) }
                .last() should beInstanceOf<Error.TokenError>()
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
                "+ - * / < > <= >=",
                LToken.Operator.Add,
                LToken.Operator.Sub,
                LToken.Operator.Multiply,
                LToken.Operator.Div,
                LToken.Operator.Lower,
                LToken.Operator.Greater,
                LToken.Operator.LowerEqual,
                LToken.Operator.GreaterEqual,
                LToken.Eof,
            )
        }
        test("Keywords") {

            val input = "pair? eq? atom? null? nil? zero? def defun define list not and or"
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
                FunToken.BuiltIn.Zero,
                FunToken.BuiltIn.DeFun,
                FunToken.BuiltIn.DeFun,
                FunToken.BuiltIn.DeFun,
                FunToken.BuiltIn.List,
                FunToken.BuiltIn.Not,
                FunToken.BuiltIn.And,
                FunToken.BuiltIn.Or,
            ).zip(tokens) { ref, actual ->
                actual should beInstanceOf<LToken.Text>()
                ref shouldBe (actual as LToken.Text).tryMatchFun()
            }
        }
        test("Text and numbers") {
            runNextTokenTest(
                "( ' fun3 1234 -cons - -1234) if)",
                LToken.Bracket.Opened,
                LToken.Quote,
                LToken.Text("fun3"),
                LToken.Number(1234),
                LToken.Operator.Sub,
                LToken.Text("cons"),
                LToken.Operator.Sub,
                LToken.Number(-1234),
                LToken.Bracket.Closed,
                LToken.Text("if"),
                LToken.Bracket.Closed,
                LToken.Eof,
            )
        }
        context("Comments") {
            test("Single line") {
                runNextTokenTest(
                    "; Hello there",
                    LToken.Eof,
                )
                runNextTokenTest(
                    """
                        1
                        ; 2
                        3
                    """,
                    LToken.Number(1),
                    LToken.Number(3),
                    LToken.Eof,
                )
            }
            test("Multiline") {
                runNextTokenTest(
                    """
                        #|
                        Hello there
                        |#
                    """,
                    LToken.Eof,
                )
                runNextTokenTest(
                    """
                        1
                        #|
                        2 |
                        |#
                        3
                    """,
                    LToken.Number(1),
                    LToken.Number(3),
                    LToken.Eof,
                )
                failTokenTest("""#""")
                failTokenTest("""|""")
                failTokenTest("""#|""")
                failTokenTest("""|#""")
                failTokenTest("""#||""")
                failTokenTest("""#|#""")
                failTokenTest("""||#""")
            }
        }
    }
})
