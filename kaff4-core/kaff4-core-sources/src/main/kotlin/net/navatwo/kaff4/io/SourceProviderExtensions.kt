package net.navatwo.kaff4.io

import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer

fun SourceProvider<Source>.buffer(): SourceProvider<BufferedSource> {
  return transform { source ->
    source as? BufferedSource ?: source.buffer()
  }
}

inline fun <T, SOURCE : Source> SourceProvider<SOURCE>.use(
  position: Long = 0L,
  timeout: Timeout = Timeout.NONE,
  block: (source: SOURCE) -> T,
): T {
  return source(position, timeout).use { block(it) }
}

inline fun <T, SOURCE : Source> SourceProvider<SOURCE>.use(
  timeout: Timeout,
  block: (source: SOURCE) -> T,
): T {
  return source(timeout).use { block(it) }
}
