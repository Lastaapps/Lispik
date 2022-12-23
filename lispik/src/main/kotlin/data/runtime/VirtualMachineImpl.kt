package data.runtime

import arrow.core.Validated
import arrow.core.handleError
import arrow.core.invalid
import arrow.core.valid
import domain.VirtualMachine
import domain.model.ByteCode
import domain.model.Error
import domain.model.LCodeQueue
import domain.model.LDump
import domain.model.LEnvironment
import domain.model.LStack
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class VirtualMachineImpl : VirtualMachine {
    override fun runCode(srcCode: ByteCode.CodeBlock): Validated<Error, ImmutableList<ByteCode.Literal>> {
        val stack = LStack()
        val dump = LDump()
        val code = LCodeQueue().also {
            it.addAll(srcCode.instructions)
        }
        val env = LEnvironment

        debugPrint(stack, dump, code, env)

        while (code.isNotEmpty()) {
            when (val inst = code.removeFirst()) {
                is ByteCode.Instruction ->
                    inst.process(stack, dump, code, env)
                        .also { debugPrint(stack, dump, code, env) }
                        .handleError { return it.invalid() }

                else -> return Error.ExecutionError.NonInstructionOccurred(inst).invalid()
            }
        }

        return stack.toImmutableList().valid()
    }

    private fun debugPrint(stack: LStack, dump: LDump, code: LCodeQueue, env: LEnvironment) {
        println("----------------------------------------------------------------")
        println("Stack: $stack")
        println("Dump:  $dump")
        println("Code:  $code")
        // println("Env:   $env")
    }
}
