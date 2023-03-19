package com.github.nava2.aff4

import org.assertj.core.api.AbstractThrowableAssert

inline fun <reified T : Throwable> AbstractThrowableAssert<*, out Throwable>.isInstanceOf():
  AbstractThrowableAssert<*, out T> {
  @Suppress("UNCHECKED_CAST")
  return isInstanceOf(T::class.java) as AbstractThrowableAssert<*, out T>
}

@Suppress("UNCHECKED_CAST")
fun AbstractThrowableAssert<*, *>.isIllegalArgumentException(
  message: String,
): AbstractThrowableAssert<*, out IllegalArgumentException> {
  return isInstanceOf<IllegalArgumentException>()
    .hasMessage(message) as AbstractThrowableAssert<*, out IllegalArgumentException>
}

@Suppress("UNCHECKED_CAST")
fun AbstractThrowableAssert<*, out Throwable>.isIllegalStateException(
  message: String,
): AbstractThrowableAssert<*, out IllegalStateException> {
  return isIllegalStateException()
    .hasMessage(message) as AbstractThrowableAssert<*, IllegalStateException>
}

fun AbstractThrowableAssert<*, out Throwable>.isIllegalStateException():
  AbstractThrowableAssert<*, out IllegalStateException> {
  return isInstanceOf()
}
