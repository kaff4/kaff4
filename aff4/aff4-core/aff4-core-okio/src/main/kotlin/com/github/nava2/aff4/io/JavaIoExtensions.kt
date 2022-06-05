package com.github.nava2.aff4.io

inline fun <T : AutoCloseable> T.applyAndCloseOnThrow(block: T.() -> Unit): T {
  runAndCloseOnThrow(block)
  return this
}

inline fun <T : AutoCloseable, R> T.runAndCloseOnThrow(block: T.() -> R): R {
  return try {
    block()
  } catch (@Suppress("TooGenericExceptionCaught") ex: Exception) {
    try {
      close()
    } catch (@Suppress("TooGenericExceptionCaught") closeEx: Exception) {
      ex.addSuppressed(closeEx)
    }

    throw ex
  }
}
