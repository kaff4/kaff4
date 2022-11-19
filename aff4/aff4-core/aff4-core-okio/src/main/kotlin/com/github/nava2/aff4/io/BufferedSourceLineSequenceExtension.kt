package com.github.nava2.aff4.io

import okio.BufferedSource

fun BufferedSource.lineSequence(): Sequence<String> = sequence {
  var targetLine = readUtf8Line()
  while (targetLine != null) {
    yield(targetLine)
    targetLine = readUtf8Line()
  }
}