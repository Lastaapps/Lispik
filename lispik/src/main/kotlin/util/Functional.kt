package util

import arrow.core.Either
import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.Validated
import arrow.core.left
import arrow.core.orElse
import arrow.core.right
import java.io.File

fun <A, B> Iterable<() -> Validated<A, B>>.reduced() =
    reduce { acc, func -> { acc().orElse { func() } } }

fun <I, V> Validated.Companion.conditionally(
    predicate: Boolean,
    invalid: () -> Invalid<I>,
    valid: () -> Valid<V>,
): Validated<I, V> =
    if (predicate) valid() else invalid()

fun loadFile(filename: String): Either<Exception, String> =
    try {
        File(filename).readText().right()
    } catch (e: Exception) {
        e.left()
    }
