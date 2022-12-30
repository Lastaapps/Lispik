package domain.model

sealed interface FunToken {
    /** Keyword */
    enum class BuiltIn(vararg val names: String) : FunToken {
        Cons("cons"),
        Car("car"),
        Cdr("cdr"),
        Nil("nil", "null"),
        If("if"),
        IsEq("eq?", "equal?"),
        IsPair("pair?"),
        IsAtom("atom?"),
        IsNil("nil?", "null?"),
        Zero("zero?"),
        Not("not"),
        And("and"),
        Or("or"),
        Lambda("lambda", "λ"),
        Apply("apply"),
        Let("let"),
        LetRec("letrec"),
        DeFun("defun", "def", "define"),
        Print("print"),
        Read("read"),
        List("list"),
    }

    @JvmInline
    value class User(val name: String) : FunToken {
        override fun toString(): String = "[$name]"
    }
}

fun LToken.Text.tryMatchFun() =
    FunToken.BuiltIn.values().firstOrNull { name in it.names } ?: FunToken.User(name)
