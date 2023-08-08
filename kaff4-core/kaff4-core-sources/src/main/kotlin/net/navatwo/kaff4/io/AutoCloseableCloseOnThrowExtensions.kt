@file:InternalApi

package net.navatwo.kaff4.io

import net.navatwo.kaff4.api.InternalApi

inline fun <T : AutoCloseable> T.applyAndCloseOnThrow(block: T.() -> Unit): T {
  runAndCloseOnThrow(block)
  return this
}

inline fun <T : AutoCloseable, R> T.runAndCloseOnThrow(block: T.() -> R): R {
  return alsoCloseOnThrow { block() }
}

inline fun <T : AutoCloseable, R> T.alsoCloseOnThrow(block: (T) -> R): R {
  return try {
    block(this)
  } catch (@Suppress("TooGenericExceptionCaught") ex: Exception) {
    try {
      close()
    } catch (@Suppress("TooGenericExceptionCaught") closeEx: Exception) {
      ex.addSuppressed(closeEx)
    }

    throw ex
  }
}
