package net.navatwo.kaff4.streams.compression

import java.nio.ByteBuffer

object ByteBuffers {
  inline fun <R> markAndReset(byteBuffer1: ByteBuffer, byteBuffer2: ByteBuffer, block: () -> R): R {
    return byteBuffer1.markAndReset {
      byteBuffer2.markAndReset {
        block()
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
