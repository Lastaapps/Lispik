package domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

sealed interface Node {
    data class CallByName(val name: String, val args: ImmutableList<Node>) : Node
    data class VariableSubstitution(val name: String) : Node

    sealed interface Nullary : Node {
        data object Read : Nullary
    }

    sealed interface Unary : Node {
        val arg0: Node

        data class IsNil(override val arg0: Node) : Unary
        data class IsPair(override val arg0: Node) : Unary
        data class IsAtom(override val arg0: Node) : Unary
        data class Car(override val arg0: Node) : Unary
        data class Cdr(override val arg0: Node) : Unary
        data class Print(override val arg0: Node) : Unary
    }

    sealed interface Binary : Node {
        val arg0: Node
        val arg1: Node

        data class IsEqual(override val arg0: Node, override val arg1: Node) : Binary
        data class Add(override val arg0: Node, override val arg1: Node) : Binary
        data class Subtract(override val arg0: Node, override val arg1: Node) : Binary
        data class Multiply(override val arg0: Node, override val arg1: Node) : Binary
        data class Divide(override val arg0: Node, override val arg1: Node) : Binary
        data class Greater(override val arg0: Node, override val arg1: Node) : Binary
        data class Lower(override val arg0: Node, override val arg1: Node) : Binary
        data class Cons(override val arg0: Node, override val arg1: Node) : Binary
    }

    sealed interface Ternary : Node {
        val arg0: Node
        val arg1: Node
        val arg2: Node

        data class If(override val arg0: Node, override val arg1: Node, override val arg2: Node) : Ternary
    }

    sealed interface Closures : Node {
        val body: Node

        data class Let(
            val name: String, val value: Node, override val body: Node,
        ) : Closures

        data class LetRec(
            val name: String, val value: Node, override val body: Node,
        ) : Closures

        data class DeFun(
            val name: String, val params: ImmutableList<String>, override val body: Node,
        ) : Closures

        data class Lambda(val params: ImmutableList<String>, override val body: Node) : Closures
    }

    sealed interface Apply : Node {
        val args: ImmutableList<Node>

        data class ApplyCall(val toCall: String, override val args: ImmutableList<Node>) : Apply {
            constructor(toCall: String, vararg args: Node) : this(toCall, args.toList().toPersistentList())
        }

        data class ApplyOperator(val operator: LToken.Operator, override val args: ImmutableList<Node>) : Apply {
            constructor(operator: LToken.Operator, vararg args: Node) : this(operator, args.toList().toPersistentList())
        }
    }

    sealed interface Literal : Node {
        data class LInteger(val value: Int) : Literal
        data class LList(val value: ImmutableList<Literal>) : Literal {
            constructor(vararg args: Literal) : this(args.toList().toImmutableList())
        }

        data object LNil : Literal
    }
}
