package net.navatwo.kaff4.io

import okio.Buffer
import okio.ByteString

fun BufferedSource.readFully(sink: Buffer, byteCount: Long) = asOkio().readFully(sink, byteCount)

fun BufferedSource.readByteString(byteCount: Long): ByteString = asOkio().readByteString(byteCount)

fun BufferedSource.readByteString(): ByteString = asOkio().readByteString()
