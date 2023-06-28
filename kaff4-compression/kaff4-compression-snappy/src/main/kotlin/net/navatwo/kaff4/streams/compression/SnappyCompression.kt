package net.navatwo.kaff4.streams.compression

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.navatwo.kaff4.model.rdf.CompressionMethod
import net.navatwo.kaff4.model.rdf.CompressionMethod.Companion.NOT_UNCOMPRESSED_SENTINEL_VALUE
import org.xerial.snappy.Snappy
import java.nio.ByteBuffer

@Singleton
class SnappyCompression @Inject internal constructor() : CompressionMethod {
  override val method: String = IDENTIFIER

  override fun isCompressed(compressed: ByteBuffer): Boolean = Snappy.isValidCompressedBuffer(compressed)

  override fun compress(source: ByteBuffer, destination: ByteBuffer): Int {
    return Snappy.compress(source, destination)
  }

  override fun uncompress(source: ByteBuffer, destination: ByteBuffer): Int {
    return if (isCompressed(source)) {
      Snappy.uncompress(source, destination)
    } else {
      NOT_UNCOMPRESSED_SENTINEL_VALUE
    }
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
