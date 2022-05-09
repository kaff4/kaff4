package com.github.nava2.aff4.io

import okio.BufferedSource
import okio.Source
import okio.buffer
import java.io.InputStream

fun SourceProvider<Source>.buffer(): SourceProvider<BufferedSource> {
  return transform { source ->
    source as? BufferedSource ?: source.buffer()
  }
}

inline fun <T, SOURCE : Source> SourceProvider<SOURCE>.use(block: (source: SOURCE) -> T) {
  return get().use { block(it) }
}

inline fun <T, SOURCE : Source> SourceProvider<SOURCE>.useAsInputStream(block: (input: InputStream) -> T) {
  return buffer().use { source ->
    source.inputStream().use { block(it) }
  }
}
