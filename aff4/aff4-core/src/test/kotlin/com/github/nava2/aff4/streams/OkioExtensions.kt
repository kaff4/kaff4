package com.github.nava2.aff4.streams

import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import java.util.function.Consumer

fun Byte.repeatByteString(length: Int): ByteString {
  val bytes = ByteArray(length) { this }
  return bytes.toByteString()
}

fun Int.repeatByteString(length: Int): ByteString {
  check(this.toUByte() in UByte.MIN_VALUE..UByte.MAX_VALUE)

  return toByte().repeatByteString(length)
}

fun ObjectAssert<BufferedSource>.md5(byteCount: Long, md5: String): ObjectAssert<BufferedSource> {
  return satisfies(Consumer { source: BufferedSource ->
    Buffer().use { readSink ->
      source.readFully(readSink, byteCount)
      assertThat(readSink.size).isEqualTo(byteCount)
      assertThat(readSink.md5()).isEqualTo(md5.decodeHex())
    }
  })
}

fun ObjectAssert<BufferedSource>.sha1(byteCount: Long, sha1: String): ObjectAssert<BufferedSource> {
  return satisfies(Consumer { source: BufferedSource ->
    Buffer().use { readSink ->
      source.readFully(readSink, byteCount)
      assertThat(readSink.size).isEqualTo(byteCount)
      assertThat(readSink.sha1()).isEqualTo(sha1.decodeHex())
    }
  })
}