package com.github.nava2.aff4.model.rdf

import com.github.nava2.aff4.io.repeatByteString
import okio.ByteString.Companion.toByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class NoneCompressionMethodTest {

  @Test
  fun compress() {
    val uncompressedBuffer = ByteBuffer.allocate(10)
    uncompressedBuffer.put(ByteArray(10) { it.toByte() })
    uncompressedBuffer.rewind()

    val compressedBuffer = ByteBuffer.allocate(20)
    assertThat(compressedBuffer.toByteString()).isEqualTo(0.repeatByteString(20))
    compressedBuffer.rewind()

    compressedBuffer.position(5)

    assertThat(CompressionMethod.None.compress(uncompressedBuffer, compressedBuffer)).isEqualTo(10)

    assertThat(uncompressedBuffer.position()).isEqualTo(0)
    assertThat(uncompressedBuffer.remaining()).isEqualTo(10)
    assertThat(uncompressedBuffer.limit()).isEqualTo(10)

    assertThat(compressedBuffer.position()).isEqualTo(5)
    assertThat(compressedBuffer.remaining()).isEqualTo(15)
    assertThat(compressedBuffer.limit()).isEqualTo(20)

    assertThat(compressedBuffer.slice(5, 10).toByteString()).isEqualTo(uncompressedBuffer.toByteString())
  }

  @Test
  fun uncompress() {
    val compressedBuffer = ByteBuffer.allocate(10)
    compressedBuffer.put(ByteArray(10) { it.toByte() })
    compressedBuffer.rewind()

    val uncompressedBuffer = ByteBuffer.allocate(20)
    assertThat(uncompressedBuffer.toByteString()).isEqualTo(0.repeatByteString(20))
    uncompressedBuffer.rewind()

    uncompressedBuffer.position(5)

    assertThat(CompressionMethod.None.uncompress(compressedBuffer, uncompressedBuffer)).isEqualTo(10)

    assertThat(compressedBuffer.position()).isEqualTo(0)
    assertThat(compressedBuffer.remaining()).isEqualTo(10)
    assertThat(compressedBuffer.limit()).isEqualTo(10)

    assertThat(uncompressedBuffer.position()).isEqualTo(5)
    assertThat(uncompressedBuffer.remaining()).isEqualTo(15)
    assertThat(uncompressedBuffer.limit()).isEqualTo(20)

    assertThat(uncompressedBuffer.slice(5, 10).toByteString()).isEqualTo(compressedBuffer.toByteString())
  }
}
