package net.navatwo.kaff4.io

import okio.ByteString
import okio.ByteString.Companion.toByteString

fun Byte.repeatByteString(length: Int): ByteString {
  val bytes = ByteArray(length) { this }
  return bytes.toByteString()
}

fun Int.repeatByteString(length: Int): ByteString {
  check(this.toUByte() in UByte.MIN_VALUE..UByte.MAX_VALUE)

  return toByte().repeatByteString(length)
}
