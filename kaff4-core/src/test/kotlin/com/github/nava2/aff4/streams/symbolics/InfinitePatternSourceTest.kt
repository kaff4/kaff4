package com.github.nava2.aff4.streams.symbolics

import com.github.nava2.aff4.io.repeatByteString
import okio.ByteString
import okio.ByteString.Companion.encode
import okio.Timeout
import okio.buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InfinitePatternSourceTest {
  @Test
  fun `pattern breaks and repeats across boundary`() {
    patternSource("abc", 4).use { source ->
      assertThat(source.readByteString(6)).isEqualTo("abcaab".encode(Charsets.US_ASCII))
      assertThat(source.readByteString(6)).isEqualTo("caabca".encode(Charsets.US_ASCII))
    }
  }

  @Test
  fun `single char is repeated with buffering`() {
    byteSource(0, 1024).use { source ->
      assertThat(source.readByteString(1 * 1024)).isEqualTo(0.repeatByteString(1 * 1024))
      assertThat(source.readByteString(2 * 1024)).isEqualTo(0.repeatByteString(2 * 1024))
    }
  }

  private fun patternSource(pattern: String, boundary: Int) = InfinitePatternSource(
    pattern = pattern.encode(Charsets.US_ASCII),
    patternBuffer = pattern.encode(Charsets.US_ASCII).asByteBuffer(),
    repetitionBoundary = boundary,
    timeout = Timeout.NONE,
  ).buffer()

  private fun byteSource(byte: Byte, bufferSize: Int) = InfinitePatternSource(
    pattern = ByteString.of(byte),
    patternBuffer = byte.repeatByteString(bufferSize).asByteBuffer(),
    repetitionBoundary = bufferSize,
    timeout = Timeout.NONE,
  ).buffer()
}
