package net.navatwo.kaff4.io

import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.Source
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import kotlin.reflect.KCallable

fun <SELF : AbstractObjectAssert<SELF, T>, T : Source> SELF.md5(byteCount: Long, md5: String): SELF {
  return md5(byteCount, md5.decodeHex())
}

fun <SELF : AbstractObjectAssert<SELF, T>, T : Source> SELF.md5(byteCount: Long, md5: ByteString): SELF {
  computeHashAssert(byteCount, Buffer::md5)
    .isEqualTo(md5)

  return this
}

fun <SELF : AbstractObjectAssert<SELF, T>, T : Source> SELF.sha1(byteCount: Long, sha1: String): SELF {
  computeHashAssert(byteCount, Buffer::sha1)
    .isEqualTo(sha1.decodeHex())

  return this
}

private fun <SELF, T> SELF.computeHashAssert(
  byteCount: Long,
  computeHash: KCallable<ByteString>,
): AbstractObjectAssert<*, ByteString>
  where SELF : AbstractObjectAssert<out SELF, T>, T : Source {
  lateinit var sourceString: String
  return `as` { "${computeHash.name}($sourceString)" }
    .extracting { source: T ->
      sourceString = source.toString()
      Buffer().use { readSink ->
        readSink.write(source, byteCount)
        assertThat(readSink.size).isEqualTo(byteCount)
        computeHash.call(readSink)
      }
    }
}
