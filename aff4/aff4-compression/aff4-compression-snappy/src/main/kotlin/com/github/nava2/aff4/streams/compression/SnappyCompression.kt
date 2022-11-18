package com.github.nava2.aff4.streams.compression

import com.github.nava2.aff4.model.rdf.CompressionMethod
import org.xerial.snappy.Snappy
import java.nio.ByteBuffer

object SnappyCompression : CompressionMethod {
  override val method: String = "http://code.google.com/p/snappy/"

  override fun compress(uncompressed: ByteBuffer, compressed: ByteBuffer): Int {
    return Snappy.compress(uncompressed, compressed)
  }

  override fun uncompress(compressed: ByteBuffer, uncompressed: ByteBuffer): Int {
    if (!Snappy.isValidCompressedBuffer(compressed)) return 0

    return Snappy.uncompress(compressed, uncompressed)
  }

  override fun toString(): String = SnappyCompression::class.java.simpleName

  override fun equals(other: Any?): Boolean {
    if (this === other) return true

    return other is CompressionMethod && method == other.method
  }

  override fun hashCode(): Int {
    return method.hashCode()
  }
}
