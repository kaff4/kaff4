package net.navatwo.kaff4.streams.compression.deflate

import net.navatwo.kaff4.model.rdf.CompressionMethod
import net.navatwo.kaff4.streams.compression.ByteBuffers.markAndReset
import java.nio.ByteBuffer
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeflateCompression @Inject internal constructor() : CompressionMethod {
  override val method: String = IDENTIFIER

  override fun isCompressed(compressed: ByteBuffer): Boolean {
    if (compressed.remaining() < MINIMUM_ZLIB_STREAM_SIZE) return false

    return compressed.markAndReset {
      val cmf = compressed.get()
      cmf == DEFLATE_MAGIC_NUMBER
    }
  }

  override fun compress(source: ByteBuffer, destination: ByteBuffer): Int {
    return markAndReset(source, destination) {
      val deflater = Deflater(DEFLATE_COMPRESSION_LEVEL)
      deflater.setInput(source)
      deflater.finish()

      val bytesDeflated = deflater.deflate(destination)

      deflater.end()

      bytesDeflated
    }
  }

  override fun uncompress(source: ByteBuffer, destination: ByteBuffer): Int {
    if (!isCompressed(source)) return 0

    return markAndReset(source, destination) {
      val inflater = Inflater()
      inflater.setInput(source)

      val inflatedBytes = inflater.inflate(destination)

      inflater.end()
      inflatedBytes
    }
  }

  override fun toString(): String = DeflateCompression::class.java.simpleName

  override fun equals(other: Any?): Boolean {
    if (this === other) return true

    return other is CompressionMethod && method == other.method
  }

  override fun hashCode(): Int {
    return method.hashCode()
  }

  companion object {
    const val IDENTIFIER = "https://tools.ietf.org/html/rfc1951"

    private const val DEFLATE_COMPRESSION_LEVEL = Deflater.BEST_COMPRESSION

    private const val DEFLATE_MAGIC_NUMBER: Byte = 0x78

    /* CMF = 1, FLG = 1, adler = 4 */
    private const val MINIMUM_ZLIB_STREAM_SIZE = 6
  }
}
