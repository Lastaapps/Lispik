package util

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

class MyStack<T> : ImmutableList<T> {

    private val backing = mutableListOf<T>()
    private val reversed get() = backing.asReversed()

    override val size: Int
        get() = backing.size

    override fun get(index: Int): T = reversed[index]

    override fun indexOf(element: T): Int = reversed.indexOf(element)

    override fun isEmpty(): Boolean = backing.isEmpty()

    override fun iterator(): Iterator<T> = reversed.toList().toPersistentList().iterator()

    override fun listIterator(): ListIterator<T> = reversed.listIterator()

    override fun listIterator(index: Int): ListIterator<T> = reversed.listIterator(index)

    override fun lastIndexOf(element: T): Int = reversed.lastIndexOf(element)

    override fun containsAll(elements: Collection<T>): Boolean =
        backing.containsAll(elements)

    override fun contains(element: T): Boolean =
        backing.contains(element)

    fun peek(): T = backing.last()
    fun pop(): T = backing.removeLast()

    fun push(item: T) = backing.add(item)

    fun clear() = backing.clear()

    fun addAll(elements: Collection<T>) = backing.addAll(elements.reversed())

    override fun toString(): String = backing.toString()
}
