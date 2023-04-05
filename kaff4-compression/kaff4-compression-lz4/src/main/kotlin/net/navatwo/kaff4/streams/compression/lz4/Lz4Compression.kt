package net.navatwo.kaff4.streams.compression.lz4

import com.google.common.io.ByteStreams
import net.navatwo.kaff4.model.rdf.CompressionMethod
import net.navatwo.kaff4.streams.compression.Streams.useAsInputStream
import net.navatwo.kaff4.streams.compression.Streams.useAsOutputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Lz4Compression @Inject internal constructor() : CompressionMethod {
  override val method: String = IDENTIFIER

  override fun compress(uncompressed: ByteBuffer, compressed: ByteBuffer): Int {
    compressed.mark()
    uncompressed.mark()

    try {
      return uncompressed.useAsInputStream { uncompressedOutput ->
        compressed.useAsOutputStream { compressedOutput ->
          FramedLZ4CompressorOutputStream(compressedOutput).use { compressingStream ->
            ByteStreams.copy(uncompressedOutput, compressingStream).toInt()
          }
        }
      }
    } finally {
      compressed.reset()
      uncompressed.reset()
    }
  }

  override fun uncompress(compressed: ByteBuffer, uncompressed: ByteBuffer): Int {
    if (!isDataCompressed(compressed)) return 0

    compressed.mark()
    uncompressed.mark()
    try {
      return compressed.useAsInputStream { compressedInput ->
        uncompressed.useAsOutputStream { uncompressedOutput ->
          FramedLZ4CompressorInputStream(compressedInput).use { decompressingStream ->
            ByteStreams.copy(decompressingStream, uncompressedOutput).toInt()
          }
        }
      }
    } finally {
      compressed.reset()
      uncompressed.reset()
    }
  }

  override fun toString(): String = Lz4Compression::class.java.simpleName

  override fun equals(other: Any?): Boolean {
    if (this === other) return true

    return other is CompressionMethod && method == other.method
  }

  override fun hashCode(): Int {
    return method.hashCode()
  }

  companion object {
    const val IDENTIFIER = "https://code.google.com/p/lz4/"

    private const val LZ4_MAGIC_NUMBER = 0x184D2204
    private const val LZ4_MAGIC_NUMBER_SIZE_BYTES = Int.SIZE_BYTES

    fun isDataCompressed(compressed: ByteBuffer): Boolean {
      if (compressed.remaining() < LZ4_MAGIC_NUMBER_SIZE_BYTES) return false

      compressed.mark()
      val originalOrder = compressed.order()
      try {
        val magicNumberHeader = compressed.order(ByteOrder.LITTLE_ENDIAN).getInt()
        return magicNumberHeader == LZ4_MAGIC_NUMBER
      } finally {
        compressed.order(originalOrder)
        compressed.reset()
      }
    }
  }
}
