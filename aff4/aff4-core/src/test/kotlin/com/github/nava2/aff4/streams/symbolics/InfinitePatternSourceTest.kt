package com.github.nava2.aff4.streams.symbolics

import com.github.nava2.aff4.streams.repeatByteString
import okio.Buffer
import okio.ByteString.Companion.encode
import okio.buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class InfinitePatternSourceTest {
  @Test
  fun `pattern breaks and repeats across boundary`() {
    patternSource("abc", 4).use { source ->
      Buffer().use { buffer ->
        source.readFully(buffer, 6)
        assertThat(buffer.readByteString()).isEqualTo("abcaab".encode(Charsets.US_ASCII))

        source.readFully(buffer, 6)
        assertThat(buffer.readByteString()).isEqualTo("caabca".encode(Charsets.US_ASCII))
      }
    }
  }

  @Test
  fun `single char is repeated with buffering`() {
    byteSource(0, 1024).use { source ->
      Buffer().use { buffer ->
        assertThat(source.read(buffer, 2 * 1024)).isEqualTo(1024)
        assertThat(buffer.readByteString()).isEqualTo(0.repeatByteString(1 * 1024))

        source.readFully(buffer, 2 * 1024)
        assertThat(buffer.readByteString()).isEqualTo(0.repeatByteString(2 * 1024))
      }
    }
  }

  private fun patternSource(pattern: String, boundary: Int) = InfinitePatternSource(
    patternBuffer = pattern.encode(Charsets.US_ASCII).asByteBuffer(),
    repetitionBoundary = boundary,
  ).buffer()

  private fun byteSource(byte: Byte, bufferSize: Int) = InfinitePatternSource(
    patternBuffer = byte.repeatByteString(bufferSize).asByteBuffer(),
    repetitionBoundary = bufferSize,
  ).buffer()
}
