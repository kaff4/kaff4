package net.navatwo.kaff4.io

import net.navatwo.kaff4.satisfies
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat

fun Byte.repeatByteString(length: Int): ByteString {
  val bytes = ByteArray(length) { this }
  return bytes.toByteString()
}

fun Int.repeatByteString(length: Int): ByteString {
  check(this.toUByte() in UByte.MIN_VALUE..UByte.MAX_VALUE)

  return toByte().repeatByteString(length)
}

fun <SELF : AbstractAssert<out SELF, ByteString>> SELF.md5(md5: String): SELF {
  return `as` { "md5($md5)" }
    .satisfies { byteString ->
      assertThat(byteString.md5()).isEqualTo(md5.decodeHex())
    }
}
