package data.compiler

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import arrow.core.valueOr
import domain.model.ByteCode
import domain.model.ByteInstructions
import domain.model.Error
import domain.model.Node
import kotlinx.collections.immutable.persistentListOf

/*
 * (let (x 1) x)
 * NIL LDC 1 CONS LDF ( LD (0.0) ) AP
 */
fun Node.Closures.Let.compile(context: CompilationContext): Validated<Error, ByteCode.CodeBlock> {
    val childContext = context.add(0, persistentListOf(name))

    val valueCode = value.compileDispatcher(context).valueOr { return it.invalid() } // child context in letrec
    val bodyCode = body.compileDispatcher(childContext).valueOr { return it.invalid() } // child context in letrec

    return listOf(
        ByteInstructions.Nil, valueCode, ByteInstructions.Cons, // create closure
        ByteInstructions.Ldf,
        ByteCode.CodeBlock(
            listOf(bodyCode, ByteInstructions.Rtn).flatten(),
        ), // pushes body to stack
        ByteInstructions.Ap, // execute
    ).flatten().valid()
}
