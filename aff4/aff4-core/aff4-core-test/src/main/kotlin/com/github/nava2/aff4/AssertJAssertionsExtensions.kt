package com.github.nava2.aff4

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.AbstractThrowableAssert
import java.util.function.Consumer

inline fun <SELF, ACTUAL> AbstractAssert<SELF, ACTUAL>.satisfies(crossinline consumer: (ACTUAL) -> Unit): SELF
  where SELF : AbstractAssert<SELF, ACTUAL> = satisfies(Consumer { consumer(it) })

fun AbstractThrowableAssert<*, *>.isIllegalArgumentException(message: String): AbstractThrowableAssert<*, *> {
  return isInstanceOf(IllegalArgumentException::class.java)
    .hasMessage(message)
}

fun AbstractThrowableAssert<*, *>.isIllegalStateException(message: String): AbstractThrowableAssert<*, *> {
  return isInstanceOf(IllegalStateException::class.java)
    .hasMessage(message)
}
