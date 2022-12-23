package data.compiler

import domain.model.ByteCode
import kotlinx.collections.immutable.toImmutableList

fun List<ByteCode>.flatten(): ByteCode.CodeBlock =
    map {
        when (it) {
            is ByteCode.CodeBlock -> it.instructions
            is ByteCode.Instruction -> listOf(it)
            is ByteCode.Literal -> listOf(it)
        }
    }.flatten().let { ByteCode.CodeBlock(it.toImmutableList()) }
