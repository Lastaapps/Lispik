package data

import arrow.core.Valid
import data.parsing.GlobalScope
import data.parsing.ParserImpl
import data.token.TokenizerImpl
import io.kotest.core.spec.style.ShouldSpec
import domain.model.Node
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import kotlinx.collections.immutable.persistentListOf

class ParserImplTest : ShouldSpec({

    context("Basic") {

        fun runParserTest(input: String, res: GlobalScope) {
            ParserImpl(TokenizerImpl.from(input)).parseToAST().let { global ->
                global.tap {
                    println("For '$input' got '$it'")
                }.tapInvalid {
                    println("For '$input' got '$it'")
                }

                global should beOfType<Valid<GlobalScope>>()
                global.map {
                    it shouldBe res
                }
            }
        }
        should("Operators") {
            """
                1
                (+ 1 (- 2 (* (/ 1 3) (< 1 2))))
            """.trimIndent().let {
                runParserTest(
                    it,
                    GlobalScope(
                        mapOf(),
                        persistentListOf(
                            Node.Literal.LInteger(1),
                            Node.Binary.Add(
                                Node.Literal.LInteger(1),
                                Node.Binary.Subtract(
                                    Node.Literal.LInteger(2),
                                    Node.Binary.Multiply(
                                        Node.Binary.Divide(
                                            Node.Literal.LInteger(1),
                                            Node.Literal.LInteger(3),
                                        ),
                                        Node.Binary.Lower(
                                            Node.Literal.LInteger(1),
                                            Node.Literal.LInteger(2),
                                        ),
                                    )
                                )
                            )
                        ),
                    )
                )
            }
        }


        should("Quote") {
            """
                (cons 1 '())
                (cons 1 '(1 2 () nil (1)))
            """.trimIndent().let {
                runParserTest(
                    it,
                    GlobalScope(
                        mapOf(),
                        persistentListOf(
                            Node.Binary.Cons(
                                Node.Literal.LInteger(1),
                                Node.Literal.LList(),
                            ),
                            Node.Binary.Cons(
                                Node.Literal.LInteger(1),
                                Node.Literal.LList(
                                    Node.Literal.LInteger(1),
                                    Node.Literal.LInteger(2),
                                    Node.Literal.LList(),
                                    Node.Literal.LNil,
                                    Node.Literal.LList(
                                        Node.Literal.LInteger(1),
                                    ),
                                ),
                            ),
                        ),
                    )
                )
            }
        }


        should("Function definition") {
            """
                (defun (foo) (+ 1 2))
                (defun (bar x y) (+ x y))
                (if 1 '(1 -2 () nil (-1)) (print (read)))
            """.trimIndent().let {
                runParserTest(
                    it,
                    GlobalScope(
                        mapOf(
                            "foo" to Node.Closures.DeFun(
                                "foo",
                                persistentListOf(),
                                Node.Binary.Add(
                                    Node.Literal.LInteger(1),
                                    Node.Literal.LInteger(2),
                                ),
                            ),
                            "bar" to Node.Closures.DeFun(
                                "bar",
                                persistentListOf("x", "y"),
                                Node.Binary.Add(
                                    Node.VariableSubstitution("x"),
                                    Node.VariableSubstitution("y"),
                                ),
                            ),
                        ),
                        persistentListOf(
                            Node.Ternary.If(
                                Node.Literal.LInteger(1),
                                Node.Literal.LList(
                                    Node.Literal.LInteger(1),
                                    Node.Literal.LInteger(-2),
                                    Node.Literal.LList(),
                                    Node.Literal.LNil,
                                    Node.Literal.LList(
                                        Node.Literal.LInteger(-1),
                                    ),
                                ),
                                Node.Unary.Print(
                                    Node.Nullary.Read,
                                ),
                            )
                        ),
                    )
                )
            }
        }


        should("Lambda and let merging") {
            """
                (let (a 1)
                    (letrec (b (+ a 1))
                        (lambda (c d) (* c (+ a b)))
                ))
            """.trimIndent().let {
                runParserTest(
                    it,
                    GlobalScope(
                        mapOf(),
                        persistentListOf(
                            Node.Closures.Let(
                                "a",
                                Node.Literal.LInteger(1),
                                Node.Closures.LetRec(
                                    "b",
                                        Node.Binary.Add(
                                            Node.VariableSubstitution("a"),
                                            Node.Literal.LInteger(1),
                                        ),
                                    Node.Closures.Lambda(
                                        persistentListOf("c", "d"),
                                        Node.Binary.Multiply(
                                            Node.VariableSubstitution("c"),
                                            Node.Binary.Add(
                                                Node.VariableSubstitution("a"),
                                                Node.VariableSubstitution("b"),
                                            )
                                        )
                                    )
                                )
                                )
                        ),
                    )
                )
            }
        }
    }
})