package domain

import arrow.core.Validated
import data.compiler.CompilerImpl
import data.parsing.GlobalScope
import domain.model.ByteCode
import domain.model.Error

interface Compiler {
    fun compile(scope: GlobalScope): Validated<Error, ByteCode>

    companion object {
        fun from() = CompilerImpl()
    }
}