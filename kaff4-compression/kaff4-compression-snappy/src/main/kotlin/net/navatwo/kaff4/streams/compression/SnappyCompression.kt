package net.navatwo.kaff4.streams.compression

import net.navatwo.kaff4.model.rdf.CompressionMethod
import org.xerial.snappy.Snappy
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnappyCompression @Inject internal constructor() : CompressionMethod {
  override val method: String = IDENTIFIER

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

  companion object {
    const val IDENTIFIER = "http://code.google.com/p/snappy/"
  }
}
