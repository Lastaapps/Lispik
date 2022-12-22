package domain

import arrow.core.Validated
import data.parsing.GlobalScope
import domain.model.Error

interface Parser {
    fun parseToAST() : Validated<Error, GlobalScope>
}