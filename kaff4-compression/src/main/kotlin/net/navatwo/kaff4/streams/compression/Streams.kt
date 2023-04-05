package net.navatwo.kaff4.streams.compression

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

object Streams {
  private const val ONE_BYTE_MASK = 0xFF

  fun <R> ByteBuffer.useAsInputStream(block: (input: InputStream) -> R): R {
    return ByteBufferBackedInputStream(this).use(block)
  }

  private class ByteBufferBackedInputStream(private val buf: ByteBuffer) : InputStream() {

    override fun read(): Int {
      return if (!buf.hasRemaining()) {
        -1
      } else {
        buf.get().toInt() and ONE_BYTE_MASK
      }
    }

    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
      if (!buf.hasRemaining()) return -1

      val readLength = len.coerceAtMost(buf.remaining())
      buf.get(bytes, off, readLength)
      return readLength
    }
  }

  fun <R> ByteBuffer.useAsOutputStream(block: (output: OutputStream) -> R): R {
    return ByteBufferBackedOutputStream(this).use(block)
  }

  private class ByteBufferBackedOutputStream(private val buffer: ByteBuffer) : OutputStream() {
    override fun write(b: Int) {
      buffer.put(b.toByte())
    }

    override fun write(bytes: ByteArray, off: Int, len: Int) {
      buffer.put(bytes, off, len)
    }
  }
}
