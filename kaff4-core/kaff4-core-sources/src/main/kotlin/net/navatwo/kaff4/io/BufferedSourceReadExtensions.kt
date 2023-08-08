package net.navatwo.kaff4.io

import okio.Buffer
import okio.ByteString
import okio.Sink
import java.nio.ByteBuffer

fun BufferedSource.read(dst: ByteBuffer): Int {
  checkTimeoutAndClosed()

  return okioSource.read(dst)
}

fun BufferedSource.readAll(sink: Sink): Long {
  checkTimeoutAndClosed()

  return okioSource.readAll(sink)
}

fun BufferedSource.readFully(sink: Buffer, byteCount: Long) {
  checkTimeoutAndClosed()

  return okioSource.readFully(sink, byteCount)
}

fun BufferedSource.lineSequence(): Sequence<String> {
  checkTimeoutAndClosed()

  return sequence {
    var targetLine = readUtf8Line()
    while (targetLine != null) {
      yield(targetLine)
      targetLine = readUtf8Line()
    }
  }
}

fun BufferedSource.readUtf8Line(): String? {
  checkTimeoutAndClosed()

  return okioSource.readUtf8Line()
}

fun BufferedSource.readByteString(byteCount: Long): ByteString {
  checkTimeoutAndClosed()
  return okioSource.readByteString(byteCount)
}

fun BufferedSource.readByteString(): ByteString {
  checkTimeoutAndClosed()
  return okioSource.readByteString()
}
