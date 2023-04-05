package net.navatwo.kaff4.streams.compression.lz4

import net.navatwo.kaff4.io.md5
import net.navatwo.kaff4.io.repeatByteString
import net.navatwo.test.GuiceModule
import okio.ByteString.Companion.toByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import javax.inject.Inject

internal class Lz4CompressionTest {
  @GuiceModule
  val testModule = Aff4Lz4Plugin

  @Inject
  private lateinit var lz4Compression: Lz4Compression

  @Test
  fun `compress and decompress round trip`() {
    val inputSize = 1024 * 10
    val byteString = 1.repeatByteString(inputSize)
    val compressedSize = 77

    val uncompressedByteBuffer = byteString.asByteBuffer()

    val compressedByteBuffer = ByteBuffer.allocateDirect(compressedSize)

    assertThat(lz4Compression.compress(uncompressedByteBuffer, compressedByteBuffer))
      .isEqualTo(inputSize)

    assertThat(uncompressedByteBuffer.position()).isZero()
    assertThat(compressedByteBuffer.position()).isZero()

    assertThat(compressedByteBuffer.toByteString()).md5("f0cad3b9abc1ed712b0d616fc8e87381")

    // truncate the compressed buffer as it is not full
    compressedByteBuffer.rewind()
    compressedByteBuffer.limit(compressedSize)

    val uncompressedRoundTripBuffer = ByteBuffer.allocateDirect(inputSize * 3 / 2)
    assertThat(lz4Compression.uncompress(compressedByteBuffer, uncompressedRoundTripBuffer))
      .isEqualTo(inputSize)

    assertThat(compressedByteBuffer.position()).isZero()
    assertThat(uncompressedRoundTripBuffer.position()).isZero()

    uncompressedRoundTripBuffer.limit(inputSize)
    assertThat(uncompressedRoundTripBuffer.toByteString()).isEqualTo(byteString)
  }

  @Test
  fun `decompress uncompressed data returns zero`() {
    val inputSize = 20 * 10
    val byteString = 1.repeatByteString(inputSize)

    val uncompressedByteBuffer = byteString.asByteBuffer()
    val outputByteBuffer = ByteBuffer.allocateDirect(inputSize * 2)

    assertThat(lz4Compression.uncompress(uncompressedByteBuffer, outputByteBuffer))
      .isEqualTo(0)

    assertThat(uncompressedByteBuffer.position()).isZero()
    assertThat(outputByteBuffer.position()).isZero()
  }
}
