package data

import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.Validated
import arrow.core.andThen
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
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.types.beInstanceOf
import java.io.BufferedReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter

private val VirtualMachineImplTest.Companion.debug get() = false

class VirtualMachineImplTest : ShouldSpec({

    fun execute(input: String): Validated<Error, List<ByteCode.Literal>> =
        input
            .also { println("Testing for input:\n$it") }
            .let { lisp -> Tokenizer.from(lisp) }
            .let { tokenizer -> Parser.from(tokenizer) }

            .parseToAST()
            .andThen { tree -> Compiler.from().compile(tree, createGlobalEnv = !debug) }
            .tap { println("Compiled to bytecode:\n$it") }
            .andThen { code ->
                VirtualMachine.from()
                    .runCode(code, debug = true, globalEnv = true)
            }
            .tap { println("Result:\n$it") }
            .tapInvalid { println("Result:\n$it") }

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

    should("Not in debug") {
        debug shouldBe false
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


        should("Checks - eq?, zero?, atom?, pair?, nil?") {
            executeAndTest("(eq?     1 1)", ByteCode.Literal.True)
            executeAndTest("(eq?     1 2)", ByteCode.Literal.False)
            executeAndTest("(zero?     0)", ByteCode.Literal.True)
            executeAndTest("(zero?     1)", ByteCode.Literal.False)
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


        should("Logic operators - not, and, or") {
            executeAndTest("(zero? 0)", ByteCode.Literal.True)
            executeAndTest("(not (zero? 0))", ByteCode.Literal.False)
            executeAndTest("(zero? 1)", ByteCode.Literal.False)
            executeAndTest("(not (zero? 1))", ByteCode.Literal.True)
            executeAndTest("(and (zero? 0) (zero? 0))", ByteCode.Literal.True)
            executeAndTest("(and (zero? 0) (zero? 1))", ByteCode.Literal.False)
            executeAndTest("(and (zero? 1) (zero? 0))", ByteCode.Literal.False)
            executeAndTest("(and (zero? 1) (zero? 1))", ByteCode.Literal.False)
            executeAndTest("(or  (zero? 0) (zero? 0))", ByteCode.Literal.True)
            executeAndTest("(or  (zero? 0) (zero? 1))", ByteCode.Literal.True)
            executeAndTest("(or  (zero? 1) (zero? 0))", ByteCode.Literal.True)
            executeAndTest("(or  (zero? 1) (zero? 1))", ByteCode.Literal.False)
        }


        should("List") {
            executeAndTest(
                """
                (list)
                (list 1 2 3)
                (list 1 2 (+ 1 2))
            """,
                ByteCode.Literal.Nil,
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
        }


        context("Print, read") {

            fun runReadPrintTest(input: String, output: String?, body: () -> Unit) {
                val reader = BufferedReader(StringReader(input))
                val writer = StringWriter()
                val out = PrintWriter(writer)

                ByteInstructions.Read.stream = reader
                ByteInstructions.Print.testStream = out

                body()

                output?.let { writer.toString() shouldBe "$output\n" }
                    ?: writer.toString().shouldBeEmpty()
            }

            should("Integer") {
                runReadPrintTest("1", "1") {
                    executeAndTest("(print (read))", ByteCode.Literal.Integer(1))
                }
            }

            should("List") {
                runReadPrintTest("(1 2 3)", "(1 2 3)") {
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
                }
            }

            should("Nil") {
                runReadPrintTest("nil", "nil") {
                    executeAndTest("(print (read))", ByteCode.Literal.Nil)
                }
            }

            should("Empty input") {
                runReadPrintTest("", "nil") {
                    executeAndTest("(print (read))", ByteCode.Literal.Nil)
                }
            }

            should("Two ints") {
                runReadPrintTest("1 1", null) {
                    executeAndFail("(print (read))")
                }
            }

            should("Two lists") {
                runReadPrintTest("(1) (1)", null) {
                    executeAndFail("(print (read))")
                }
            }

            should("Two nils") {
                runReadPrintTest("nil nil", null) {
                    executeAndFail("(print (read))")
                }
            }
        }
    }

    context("How to interpret non-linear control flow?") {
        should("If literals") {
            executeAndTest("(if 1 42 69)", listOf(ByteCode.Literal.Integer(42)))
            executeAndTest("(if 0 42 69)", listOf(ByteCode.Literal.Integer(69)))
            executeAndTest(
                "(if 1 42 69) (if 0 42 69)",
                listOf(
                    ByteCode.Literal.Integer(42),
                    ByteCode.Literal.Integer(69),
                )
            )
            executeAndTest("(if (eq? 1 1) 42 69)", listOf(ByteCode.Literal.Integer(42)))
            executeAndTest("(if (eq? 1 2) 42 69)", listOf(ByteCode.Literal.Integer(69)))
        }
        should("If nested") {
            executeAndTest(
                """
                    (if (eq? (/ 10 3) 3)
                        (if (< 1 2)
                            (+ 52 -10)
                            0)
                        (cons 1 nil))
                """,
                listOf(ByteCode.Literal.Integer(42)),
            )
        }
    }

    context("How to implement function application?") {
        context("Let") {
            should("Basic") {
                executeAndTest(
                    "(let (x 1) x)",
                    ByteCode.Literal.Integer(1),
                )
            }
            should("Two lets following") {
                executeAndTest(
                    "(let (x 1) x) (let (x 2) x)",
                    ByteCode.Literal.Integer(1),
                    ByteCode.Literal.Integer(2),
                )
            }
            should("Calculate value and evaluate body") {
                executeAndTest(
                    "(let (x (+ 1 2)) (+ x 3))",
                    ByteCode.Literal.Integer(6),
                )
            }
            should("Return value") {
                executeAndTest(
                    "(+ (let (x 1) x) 42)",
                    ByteCode.Literal.Integer(43),
                )
            }
            should("Parents variable") {
                executeAndTest(
                    "(let (y 1) (let (x 2) y))",
                    ByteCode.Literal.Integer(1),
                )
            }
            should("Shadowing") {
                executeAndTest(
                    "(let (x 1) (let (x 2) x))",
                    ByteCode.Literal.Integer(2),
                )
            }
            should("Unknown variable") {
                executeAndFail("(let (x 1) (let (x 2) y))")
            }
        }

        context("Define") {
            should("Procedure") {
                executeAndTest(
                    """
                        (define (foo) 1)
                        (foo)
                    """,
                    ByteCode.Literal.Integer(1),
                )
            }
            should("Computing procedure") {
                executeAndTest(
                    """
                        (define (foo) (+ 2 3))
                        (foo)
                    """,
                    ByteCode.Literal.Integer(5),
                )
            }
            should("Two procedures") {
                executeAndTest(
                    """
                        (define (foo) 1)
                        (define (bar) 2)
                        (foo)
                        (bar)
                        (foo)
                    """,
                    ByteCode.Literal.Integer(1),
                    ByteCode.Literal.Integer(2),
                    ByteCode.Literal.Integer(1),
                )
            }
            should("Same name") {
                executeAndFail(
                    """
                        (define (foo) 1)
                        (define (bar) 2)
                        (define (bar) 2)
                        (foo)
                    """,
                )
            }
            should("No call") {
                executeAndTest(
                    """
                        (define (foo) 1)
                        (define (bar) 2)
                    """,
                )
            }

            should("Args") {
                executeAndTest(
                    """
                        (define (foo x) x)
                        (foo 1)
                    """,
                    ByteCode.Literal.Integer(1),
                )
            }
            should("Args computation") {
                executeAndTest(
                    """
                        (define (foo x) (+ x 2))
                        (foo 1)
                    """,
                    ByteCode.Literal.Integer(3),
                )
            }
            should("Args computation more args") {
                executeAndTest(
                    """
                        (define (foo x y) (+ x y))
                        (foo 1 2)
                    """,
                    ByteCode.Literal.Integer(3),
                )
            }
            should("Unknown args") {
                executeAndFail(
                    """
                        (define (foo x y) (+ a y))
                        (foo 1 2)
                    """,
                )
            }
            // may work, no way to check with current bytecode
            should("To many args") {
                executeAndFail(
                    """
                        (define (foo x y) (+ a y))
                        (foo 1 2 3)
                    """,
                )
            }
            should("To few args") {
                executeAndFail(
                    """
                        (define (foo x y) (+ a y))
                        (foo 1)
                    """,
                )
            }
            should("No args") {
                executeAndFail(
                    """
                        (define (foo x y) (+ a y))
                        (foo)
                    """,
                )
            }
            should("No function defined") {
                executeAndFail(
                    """
                        (define (bar x y) (+ a y))
                        (foo)
                    """,
                )
            }


            should("Call another fun above") {
                executeAndTest(
                    """
                        (define (bar x) x)
                        (define (foo x) (bar x))
                        (foo 1)
                    """,
                    ByteCode.Literal.Integer(1),
                )
            }
            should("Call another fun below") {
                executeAndTest(
                    """
                        (define (foo x) (bar x))
                        (define (bar x) x)
                        (foo 1)
                    """,
                    ByteCode.Literal.Integer(1),
                )
            }
            should("Call and eval another fun above") {
                executeAndTest(
                    """
                        (define (bar x y) (+ x y))
                        (define (foo x y) (+ (bar x 4) y))
                        (foo 1 2)
                    """,
                    ByteCode.Literal.Integer(7),
                )
            }
            should("Call and eval another fun below") {
                executeAndTest(
                    """
                        (define (foo x y) (+ (bar x 4) y))
                        (define (bar x y) (+ x y))
                        (foo 1 2)
                    """,
                    ByteCode.Literal.Integer(7),
                )
            }
            should("Factorial") {
                executeAndTest(
                    """
                    (define (fact n)
                      (if (eq? n 0)
                          1
                          (* n (fact (- n 1)))))
                    (fact 0)
                    (fact 1)
                    (fact 2)
                    (fact 3)
                """,
                    ByteCode.Literal.Integer(1),
                    ByteCode.Literal.Integer(1),
                    ByteCode.Literal.Integer(2),
                    ByteCode.Literal.Integer(6),
                )
            }

            should("Fibonacci 5") {
                executeAndTest(
                    """
                        (define (fibonacci n)
                            (if (eq? n 0)
                                0
                                (if (eq? n 1)
                                    1
                                    (+ (fibonacci (- n 1)) (fibonacci (- n 2))))))
                        (fibonacci 5)
                        """,
                    ByteCode.Literal.Integer(5),
                )
            }
        }

        context("Lambda") {
            should("No args") {
                executeAndTest(
                    """
                    ((lambda () 1))
                """,
                    ByteCode.Literal.Integer(1),
                )
            }
            should("Identity") {
                executeAndTest(
                    """
                    ((lambda (x) x) 1)
                """,
                    ByteCode.Literal.Integer(1),
                )
            }
            should("With eval") {
                executeAndTest(
                    """
                    ((lambda (x) (+ x 2)) 1)
                """,
                    ByteCode.Literal.Integer(3),
                )
            }
            should("More args") {
                executeAndTest(
                    """
                    ((lambda (x y) (- x y)) 1 2)
                """,
                    ByteCode.Literal.Integer(-1),
                )
            }
            should("Store in variable") {
                executeAndTest(
                    """
                        (let (l (lambda () 1)) (l))
                    """,
                    ByteCode.Literal.Integer(1),
                )
            }
            should("Store in variable with eval") {
                executeAndTest(
                    """
                        (let (l (lambda (x) (+ x 2))) (l 1))
                    """,
                    ByteCode.Literal.Integer(3),
                )
            }
            should("Returned from fun") {
                executeAndTest(
                    """
                        (define (foo a) (lambda (x) (+ x a)))
                        ((foo 1) 2)
                    """,
                    ByteCode.Literal.Integer(3),
                )
            }
            should("Returned from lambda") {
                executeAndTest(
                    """
                        (((lambda (x)
                            (lambda (y) (+ x y))
                        ) 1) 2)
                    """,
                    ByteCode.Literal.Integer(3),
                )
            }
        }

        context("Apply") {
            should("Operator") {
                // Not yet supported
                executeAndFail("""(apply + 2 '(3))""")
                executeAndFail("""(apply - 2 '(3))""")
                executeAndFail("""(apply * '(2 3))""")
            }
            should("Call by name") {
                executeAndTest(
                    """
                        (define (max x y) 
                            (if (> x y) x y)
                        )
                        (apply max 2 3 '()) 
                        (apply max (+ 1 4) 3 nil) 
                    """,
                    ByteCode.Literal.Integer(3),
                    ByteCode.Literal.Integer(5),
                )
            }
            should("Call evaluated value") {
                executeAndTest(
                    """
                        (apply (lambda (x) x) 1 null)
                        (apply (lambda (x) x) '(1))
                    """,
                    ByteCode.Literal.Integer(1),
                    ByteCode.Literal.Integer(1),
                )
            }
        }
    }

    context("How to implement recursive functions?") {
        // Stolen from let
        should("Basic") {
            executeAndTest(
                "(letrec (x 1) x)",
                ByteCode.Literal.Integer(1),
            )
        }
        should("Two lets following") {
            executeAndTest(
                "(letrec (x 1) x) (letrec (x 2) x)",
                ByteCode.Literal.Integer(1),
                ByteCode.Literal.Integer(2),
            )
        }
        should("Calculate value and evaluate body") {
            executeAndTest(
                "(letrec (x (+ 1 2)) (+ x 3))",
                ByteCode.Literal.Integer(6),
            )
        }
        should("Return value") {
            executeAndTest(
                "(+ (letrec (x 1) x) 42)",
                ByteCode.Literal.Integer(43),
            )
        }
        should("Parents variable") {
            executeAndTest(
                "(letrec (y 1) (let (x 2) y))",
                ByteCode.Literal.Integer(1),
            )
        }
        should("Shadowing") {
            executeAndTest(
                "(letrec (x 1) (letrec (x 2) x))",
                ByteCode.Literal.Integer(2),
            )
        }
        should("Unknown variable") {
            executeAndFail("(letrec (x 1) (letrec (x 2) y))")
        }

        // Original
        should("Factorial 0") {
            executeAndTest(
                """
                    (letrec
                        (fact (lambda (n)
                            (if (eq? n 0)
                                1
                                (* n (fact (- n 1)))
                            )
                        ))
                        (fact 0))
                """,
                ByteCode.Literal.Integer(1),
            )
        }
        should("Factorial 1") {
            executeAndTest(
                """
                    (letrec
                        (fact (lambda (n)
                            (if (eq? n 0)
                                1
                                (* n (fact (- n 1)))
                            )
                        ))
                        (fact 1))
                """,
                ByteCode.Literal.Integer(1),
            )
        }
        should("Factorial 10") {
            executeAndTest(
                """
                    (letrec
                        (fact (lambda (n)
                            (if (eq? n 0)
                                1
                                (* n (fact (- n 1)))
                            )
                        ))
                        (fact 10))
                """,
                ByteCode.Literal.Integer(3628800),
            )
        }
        should("Fun with same name") {
            executeAndTest(
                """
                    (defun (fact n) -1)
                    (defun (foo n) -1)
                    (letrec
                        (fact (lambda (n)
                            (if (eq? n 0)
                                1
                                (* n (fact (- n 1)))
                            )
                        ))
                        (fact 3))
                """,
                ByteCode.Literal.Integer(6),
            )
        }
        should("Fibonacci 5") {
            executeAndTest(
                """
                    (letrec (fibonacci (lambda (n)
                        (if (eq? n 0)
                            0
                            (if (eq? n 1)
                                1
                                (+ (fibonacci (- n 1)) (fibonacci (- n 2)))))))
                        (fibonacci 5))
                """,
                ByteCode.Literal.Integer(5),
            )
        }
    }
}) {
    companion object
}
