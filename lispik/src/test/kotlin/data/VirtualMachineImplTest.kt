package data

import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.Compiler
import domain.Parser
import domain.Tokenizer
import domain.VirtualMachine
import domain.model.ByteCode
import domain.model.ByteInstructions
import domain.model.Error
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import java.io.BufferedReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter

class VirtualMachineImplTest : ShouldSpec({

    fun execute(input: String): Validated<Error, List<ByteCode.Literal>> =
        input
            .also { println("Testing for input:\n$it") }
            .let { lisp -> Tokenizer.from(lisp) }
            .let { tokenizer -> Parser.from(tokenizer) }
            .let { parser -> parser.parseToAST().valueOr { return it.invalid() } }
            .let { tree -> Compiler.from().compile(tree).valueOr { return it.invalid() } }
            .also { println("Compiled to bytecode:\n$it") }
            .let { code -> VirtualMachine.from().runCode(code).valueOr { return it.invalid() } }
            .also { println("Result:\n$it") }
            .valid()

    fun executeAndTest(input: String, expected: List<ByteCode.Literal>) {
        execute(input).let { actual ->
            actual should beInstanceOf<Valid<ByteCode>>()
            actual.tap {
                it shouldBe expected
            }
        }
    }

    fun executeAndTest(input: String, vararg expected: ByteCode.Literal): Unit =
        executeAndTest(input, expected.asList())

    fun executeAndFail(input: String) {
        execute(input).let { actual ->
            actual should beInstanceOf<Invalid<Error>>()
        }
    }

    context("How to represent lists, numbers and functions") {
        should("Empty input") {
            executeAndTest(
                "",
                emptyList(),
            )
        }
        should("Single literals") {
            executeAndTest(
                """
                    1
                    nil
                    '()
                    '(1 2)
                """,
                listOf(
                    ByteCode.Literal.Integer(1),
                    ByteCode.Literal.Nil,
                    ByteCode.Literal.Nil,
                    ByteCode.Literal.LPair(
                        ByteCode.Literal.Integer(1),
                        ByteCode.Literal.LPair(
                            ByteCode.Literal.Integer(2),
                            ByteCode.Literal.Nil,
                        )
                    ),
                ),
            )
        }
    }

    context("How to interpret simple operations (arithmetic, cons, car, …)") {
        should("Math") {
            executeAndTest(
                """
                    (+ 1 2)
                    (- 1 2)
                    (* (* 2 4) (/ -10 3))
                    (> 1 2)
                    (> 2 1)
                    (< 1 2)
                    (< 2 1)
                """,
                listOf(
                    ByteCode.Literal.Integer(3),
                    ByteCode.Literal.Integer(-1),
                    ByteCode.Literal.Integer(-24),
                    ByteCode.Literal.False,
                    ByteCode.Literal.True,
                    ByteCode.Literal.True,
                    ByteCode.Literal.False,
                ),
            )
            executeAndFail("(/ 1 0)")
            executeAndFail("(+ 1)")
            executeAndFail("(+ 1 2 3)")
        }

        should("Lists - cons") {
            executeAndTest(
                """
                    (cons 1 2)
                    (cons (+ 1 2) nil)
                    (cons 1 (cons 2 '()))
                """,
                listOf(
                    ByteCode.Literal.LPair(
                        ByteCode.Literal.Integer(1),
                        ByteCode.Literal.Integer(2),
                    ),
                    ByteCode.Literal.LPair(
                        ByteCode.Literal.Integer(3),
                        ByteCode.Literal.Nil,
                    ),
                    ByteCode.Literal.LPair(
                        ByteCode.Literal.Integer(1),
                        ByteCode.Literal.LPair(
                            ByteCode.Literal.Integer(2),
                            ByteCode.Literal.Nil,
                        ),
                    ),
                ),
            )
        }


        should("Lists - car, cdr") {
            executeAndTest(
                """
                    (car '(1))
                    (cdr '(1))
                    (car '(1 2))
                    (cdr '(1 2))
                    (car (cdr '(1 2 3)))
                    (cdr (cdr '(1 2 3)))
                """,
                listOf(
                    ByteCode.Literal.Integer(1),
                    ByteCode.Literal.Nil,
                    ByteCode.Literal.Integer(1),
                    ByteCode.Literal.LPair(
                        ByteCode.Literal.Integer(2),
                        ByteCode.Literal.Nil,
                    ),
                    ByteCode.Literal.Integer(2),
                    ByteCode.Literal.LPair(
                        ByteCode.Literal.Integer(3),
                        ByteCode.Literal.Nil,
                    ),
                ),
            )
            executeAndFail("(car nil)")
            executeAndFail("(cdr nil)")
            executeAndFail("(car 1)")
            executeAndFail("(cdr 1)")
        }


        should("Checks - eq?, atom?, pair?, nil?") {
            executeAndTest("(eq?     1 1)", ByteCode.Literal.True)
            executeAndTest("(eq?     1 2)", ByteCode.Literal.False)
            executeAndTest("(atom?     1)", ByteCode.Literal.True)
            executeAndTest("(atom?   nil)", ByteCode.Literal.False)
            executeAndTest("(atom?  '(1))", ByteCode.Literal.False)
            executeAndTest("(nil?      1)", ByteCode.Literal.False)
            executeAndTest("(nil?    nil)", ByteCode.Literal.True)
            executeAndTest("(nil?   '(1))", ByteCode.Literal.False)
            executeAndTest("(pair?     1)", ByteCode.Literal.False)
            executeAndTest("(pair?   nil)", ByteCode.Literal.False)
            executeAndTest("(pair?  '(1))", ByteCode.Literal.True)
        }


        should("Print, read - int") {
            val reader = BufferedReader(StringReader("1"))
            val writer = StringWriter()
            val out = PrintWriter(writer)

            ByteInstructions.Read.stream = reader
            ByteInstructions.Print.testStream = out

            executeAndTest("(print (read))", ByteCode.Literal.Integer(1))
            writer.toString() shouldBe "1\n"
        }

        should("Print, read - list") {
            val reader = BufferedReader(StringReader("'(1 2 3)"))
            val writer = StringWriter()
            val out = PrintWriter(writer)

            ByteInstructions.Read.stream = reader
            ByteInstructions.Print.testStream = out

            executeAndTest(
                "(print (read))",
                ByteCode.Literal.LPair(
                    ByteCode.Literal.Integer(1),
                    ByteCode.Literal.LPair(
                        ByteCode.Literal.Integer(2),
                        ByteCode.Literal.LPair(
                            ByteCode.Literal.Integer(3),
                            ByteCode.Literal.Nil,
                        )
                    )
                ),
            )
            writer.toString() shouldBe "(1 2 3)\n"
        }

        should("Print, read - nil") {
            val reader = BufferedReader(StringReader("nil"))
            val writer = StringWriter()
            val out = PrintWriter(writer)

            ByteInstructions.Read.stream = reader
            ByteInstructions.Print.testStream = out

            executeAndTest("(print (read))", ByteCode.Literal.Nil)
            writer.toString() shouldBe "nil\n"
        }

        should("Print, read - empty") {
            val reader = BufferedReader(StringReader(""))
            val writer = StringWriter()
            val out = PrintWriter(writer)

            ByteInstructions.Read.stream = reader
            ByteInstructions.Print.testStream = out

            executeAndTest("(print (read))", ByteCode.Literal.Nil)
            writer.toString() shouldBe "nil\n"
        }
    }
})
