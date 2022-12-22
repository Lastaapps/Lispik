package util

import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.Validated
import arrow.core.orElse

fun <A, B> Iterable<() -> Validated<A, B>>.reduced() =
    reduce { acc, func -> { acc().orElse { func() } } }

fun <I, V> Validated.Companion.conditionally(
    predicate: Boolean,
    invalid: () -> Invalid<I>,
    valid: () -> Valid<V>,
): Validated<I, V> =
    if (predicate) valid() else invalid()
