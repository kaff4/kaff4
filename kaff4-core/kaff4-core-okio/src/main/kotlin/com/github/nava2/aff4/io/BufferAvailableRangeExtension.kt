package com.github.nava2.aff4.io

import java.nio.Buffer

fun Buffer.availableRange(mappedPosition: Long): LongRange {
  val start = mappedPosition - position()
  val last = mappedPosition + remaining()
  return start until last
}
