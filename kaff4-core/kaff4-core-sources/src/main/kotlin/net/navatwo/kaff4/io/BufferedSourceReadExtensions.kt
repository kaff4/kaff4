package net.navatwo.kaff4.io

import okio.Sink
import java.nio.ByteBuffer

fun BufferedSource.read(dst: ByteBuffer): Int = asOkio().read(dst)

fun BufferedSource.readAll(sink: Sink): Long = asOkio().readAll(sink)
