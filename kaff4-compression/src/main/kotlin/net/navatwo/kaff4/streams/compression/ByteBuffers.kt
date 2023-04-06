package net.navatwo.kaff4.streams.compression

import java.nio.ByteBuffer

object ByteBuffers {
  inline fun <R> markAndReset(vararg byteBuffers: ByteBuffer, block: () -> R): R {
    val positions = byteBuffers.associateWith { it.position() }
    return try {
      block()
    } finally {
      for ((buffer, pos) in positions) {
        buffer.position(pos)
      }
    }
  }

  inline fun <R> ByteBuffer.markAndReset(block: () -> R): R {
    val pos = position()
    return try {
      block()
    } finally {
      position(pos)
    }
  }
}
