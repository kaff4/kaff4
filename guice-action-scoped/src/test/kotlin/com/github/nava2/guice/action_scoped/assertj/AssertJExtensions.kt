package com.github.nava2.guice.action_scoped.assertj

import org.assertj.core.api.AbstractMapAssert
import org.assertj.core.api.AbstractThrowableAssert

fun <SELF : AbstractMapAssert<SELF, ACTUAL, K, V>, ACTUAL : Map<K, V>, K, V> SELF.containsAllEntriesOf(
  vararg entries: Pair<K, V>,
): SELF {
  return containsAllEntriesOf(entries.toMap())
}

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