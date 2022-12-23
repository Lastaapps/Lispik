package domain

import arrow.core.Validated
import data.runtime.VirtualMachineImpl
import domain.model.ByteCode
import domain.model.Error
import kotlinx.collections.immutable.ImmutableList

interface VirtualMachine {
    fun runCode(srcCode: ByteCode.CodeBlock): Validated<Error, ImmutableList<ByteCode.Literal>>

    companion object {
        fun from() = VirtualMachineImpl()
    }
}