package data

import arrow.core.Invalid
import arrow.core.Valid
import data.parsing.GlobalScope
import domain.Parser
import domain.Tokenizer
import domain.model.Error
import domain.model.LToken
import domain.model.Node
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.beOfType
import kotlinx.collections.immutable.persistentListOf

class ParserImplTest : ShouldSpec({

    context("Basic") {

        fun createParser(input: String) =
            Parser.from(Tokenizer.from(input)).parseToAST().let { res ->
                println("Testing for input:\n'$input'")
                res.tap {
                    println("Result:\n'$it'")
                }.tapInvalid {
                    println("Result:\n'$it'")
                }
            }

        fun runParserTest(input: String, expected: GlobalScope) {
            createParser(input).let { actual ->
                actual should beOfType<Valid<GlobalScope>>()
                actual.map {
                    it shouldBe expected
                }
            }
        }

        fun runParserFail(input: String, onError: (Error) -> Unit) {
            createParser(input).let { actual ->
                actual should beOfType<Invalid<Error>>()
                actual.mapLeft(onError)
            }
        }

        should("Operators") {
            """
                1 nil
                (+ 1 (- 2 (* (/ 1 3) (< 1 2))))
            """.trimIndent().let {
                runParserTest(
                    it,
                    GlobalScope(
                        persistentListOf(),
                        persistentListOf(
                            Node.Literal.LInteger(1),
                            Node.Literal.LNil,
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
                        persistentListOf(),
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


        should("List") {
            """
                (list)
                (list 1 2 3)
                (list 1 2 (+ 1 2))
            """.let {
                runParserTest(
                    it,
                    GlobalScope(
                        persistentListOf(),
                        persistentListOf(
                            Node.Nnary.ListNode(),
                            Node.Nnary.ListNode(
                                Node.Literal.LInteger(1),
                                Node.Literal.LInteger(2),
                                Node.Literal.LInteger(3),
                            ),
                            Node.Nnary.ListNode(
                                Node.Literal.LInteger(1),
                                Node.Literal.LInteger(2),
                                Node.Binary.Add(
                                    Node.Literal.LInteger(1),
                                    Node.Literal.LInteger(2),
                                )
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
                        persistentListOf(
                            Node.Closures.DeFun(
                                "foo",
                                persistentListOf(),
                                Node.Binary.Add(
                                    Node.Literal.LInteger(1),
                                    Node.Literal.LInteger(2),
                                ),
                            ),
                            Node.Closures.DeFun(
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


        should("Nested defun") {
            """
                (+ 1 (defun (foo) (+ 1 2)))
            """.trimIndent().let {
                runParserFail(it) { error ->
                    error should beInstanceOf<Error.ParserError.DeFunInNonRootScope>()
                }
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
                        persistentListOf(),
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


        should("Apply") {
            """
                (apply + 2 '(3)) 
                (apply - 2 '(3)) 
                (apply * '(2 3)) 
                (apply a 2 3 '()) 
                (apply a (+ 1 1) 3 nil) 
            """.trimIndent().let {
                runParserTest(
                    it,
                    GlobalScope(
                        persistentListOf(),
                        persistentListOf(
                            Node.Apply.ApplyOperator(
                                LToken.Operator.Add,
                                Node.Literal.LInteger(2),
                                Node.Literal.LList(
                                    Node.Literal.LInteger(3),
                                ),
                            ),
                            Node.Apply.ApplyOperator(
                                LToken.Operator.Sub,
                                Node.Literal.LInteger(2),
                                Node.Literal.LList(
                                    Node.Literal.LInteger(3),
                                ),
                            ),
                            Node.Apply.ApplyOperator(
                                LToken.Operator.Times,
                                Node.Literal.LList(
                                    Node.Literal.LInteger(2),
                                    Node.Literal.LInteger(3),
                                ),
                            ),
                            Node.Apply.ApplyCall(
                                "a",
                                Node.Literal.LInteger(2),
                                Node.Literal.LInteger(3),
                                Node.Literal.LList(),
                            ),
                            Node.Apply.ApplyCall(
                                "a",
                                Node.Binary.Add(
                                    Node.Literal.LInteger(1),
                                    Node.Literal.LInteger(1),
                                ),
                                Node.Literal.LInteger(3),
                                Node.Literal.LNil,
                            ),
                        ),
                    )
                )
            }
        }

        should("Callable from eval") {
            """
                (define (foo x y)
                    (lambda (z) (+ x (+ y z))))

                (print ((foo 10 20) 30))
            """.trimIndent().let {
                runParserTest(
                    it,
                    GlobalScope(
                        persistentListOf(
                            Node.Closures.DeFun(
                                "foo",
                                persistentListOf("x", "y"),
                                Node.Closures.Lambda(
                                    persistentListOf("z"),
                                    Node.Binary.Add(
                                        Node.VariableSubstitution("x"),
                                        Node.Binary.Add(
                                            Node.VariableSubstitution("y"),
                                            Node.VariableSubstitution("z"),
                                        )
                                    )
                                ),
                            ),
                        ),
                        persistentListOf(
                            Node.Unary.Print(
                                Node.Call.ByEvaluation(
                                    Node.Call.ByName(
                                        "foo",
                                        Node.Literal.LInteger(10),
                                        Node.Literal.LInteger(20),
                                    ),
                                    Node.Literal.LInteger(30),
                                )
                            )
                        ),
                    )
                )
            }
        }
    }
})
