package com.github.nava2.aff4.io

import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.Source
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat

fun <SELF : AbstractObjectAssert<SELF, T>, T : Source> SELF.md5(byteCount: Long, md5: String): SELF {
  return md5(byteCount, md5.decodeHex())
}

fun <SELF : AbstractObjectAssert<SELF, T>, T : Source> SELF.md5(byteCount: Long, md5: ByteString): SELF {
  computeHashAssert(byteCount, Buffer::md5)
    .`as` { "md5(source[$byteCount])" }
    .isEqualTo(md5)

  return this
}

fun <SELF : AbstractObjectAssert<SELF, T>, T : Source> SELF.sha1(byteCount: Long, sha1: String): SELF {
  computeHashAssert(byteCount, Buffer::sha1)
    .`as` { "sha1(source[$byteCount])" }
    .isEqualTo(sha1.decodeHex())

  return this
}

private fun <SELF, T> SELF.computeHashAssert(
  byteCount: Long,
  computeHash: (Buffer) -> ByteString,
): AbstractObjectAssert<*, ByteString>
  where SELF : AbstractObjectAssert<SELF, T>, T : Source = extracting { source: T ->
  Buffer().use { readSink ->
    readSink.write(source, byteCount)
    assertThat(readSink.size).isEqualTo(byteCount)
    computeHash(readSink)
  }
}
