package domain.model

import arrow.core.*
import arrow.core.continuations.option
import arrow.typeclasses.Monoid

sealed interface FunToken {
    /** Keyword */
    enum class BuiltIn : LToken {
        Cons,
        Car,
        Cdr,
        Nil,
        Eq,
        Lambda,
        Apply,
        Let,
        LetRec,
        DeFun,
        Print,
        Read,
        If,
    }

    data class User(val name: String)
}

fun LToken.Text.tryMatchFun() =
    FunToken.BuiltIn.values().firstOrNull { it.name.lowercase() == name } ?: FunToken.User(name)
