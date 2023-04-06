package net.navatwo.kaff4.streams.compression.lz4

import net.navatwo.kaff4.io.md5
import net.navatwo.kaff4.io.repeatByteString
import net.navatwo.kaff4.model.rdf.CompressionMethod
import net.navatwo.kaff4.model.rdf.CompressionMethod.Companion.NOT_UNCOMPRESSED_SENTINEL_VALUE
import net.navatwo.kaff4.streams.compression.ByteBuffers.markAndReset
import okio.ByteString.Companion.toByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

@Suppress("FunctionNaming", "MagicNumber")
abstract class BaseCompressionMethodTest protected constructor() {
  protected abstract val compressionMethod: CompressionMethod

  protected abstract val compressedDataValues: CompressedDataValues

  data class CompressedDataValues(
    val compressedSize: Int,
    val md5: String,
  )

  @Test
  fun `compress and decompress round trip`() {
    val inputSize = 1024 * 10
    val byteString = 1.repeatByteString(inputSize)
    val uncompressedByteBuffer = ByteBuffer.allocateDirect(inputSize).apply {
      put(byteString.toByteArray())
      rewind()
    }

    val compressedSize = compressedDataValues.compressedSize

    val compressedByteBuffer = ByteBuffer.allocateDirect(10 + compressedSize.coerceAtMost(inputSize * 5))

    // Write into an offset
    val compressionWriteOffset = 5
    compressedByteBuffer.position(compressionWriteOffset)

    assertThat(compressionMethod.compress(uncompressedByteBuffer, compressedByteBuffer))
      .isEqualTo(compressedSize)

    assertThat(uncompressedByteBuffer.position()).isZero()
    assertThat(compressedByteBuffer.position()).isEqualTo(compressionWriteOffset)

    // It's not full
    compressedByteBuffer.limit(compressedSize + compressionWriteOffset)

    compressedByteBuffer.markAndReset {
      assertThat(compressedByteBuffer.toByteString()).md5(compressedDataValues.md5)
    }

    val uncompressedRoundTripBuffer = ByteBuffer.allocateDirect(inputSize * 3 / 2)
    val uncompressedRoundTripBufferOffset = 2
    uncompressedRoundTripBuffer.position(uncompressedRoundTripBufferOffset)

    assertThat(compressionMethod.uncompress(compressedByteBuffer, uncompressedRoundTripBuffer))
      .isEqualTo(inputSize)

    assertThat(compressedByteBuffer.position()).isEqualTo(compressionWriteOffset)
    assertThat(uncompressedRoundTripBuffer.position()).isEqualTo(uncompressedRoundTripBufferOffset)

    uncompressedRoundTripBuffer.limit(inputSize + uncompressedRoundTripBufferOffset)
    assertThat(uncompressedRoundTripBuffer.toByteString()).isEqualTo(byteString)
  }

  @Test
  fun `decompress uncompressed data returns zero`() {
    val inputSize = 20 * 10
    val byteString = 1.repeatByteString(inputSize)

    val uncompressedByteBuffer = ByteBuffer.allocateDirect(byteString.size).apply {
      put(byteString.toByteArray())
      rewind()
    }
    val outputByteBuffer = ByteBuffer.allocateDirect(inputSize * 2)

    assertThat(compressionMethod.uncompress(uncompressedByteBuffer, outputByteBuffer))
      .isEqualTo(NOT_UNCOMPRESSED_SENTINEL_VALUE)

    assertThat(uncompressedByteBuffer.position()).isZero()
    assertThat(outputByteBuffer.position()).isZero()
  }
}
