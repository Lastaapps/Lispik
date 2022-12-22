package domain.model

sealed interface FunToken {
    /** Keyword */
    enum class BuiltIn(vararg val names: String) : FunToken {
        Cons("cons"),
        Car("car"),
        Cdr("cdr"),
        Nil("nil", "null"),
        If("if"),
        IsEq("eq?"),
        IsPair("pair?"),
        IsAtom("atom?"),
        IsNil("nil?", "null?"),
        Lambda("lambda"),
        Apply("apply"),
        Let("let"),
        LetRec("letrec"),
        DeFun("defun", "def", "define"),
        Print("print"),
        Read("read"),
    }

    data class User(val name: String) : FunToken
}

fun LToken.Text.tryMatchFun() =
    FunToken.BuiltIn.values().firstOrNull { name in it.names } ?: FunToken.User(name)
