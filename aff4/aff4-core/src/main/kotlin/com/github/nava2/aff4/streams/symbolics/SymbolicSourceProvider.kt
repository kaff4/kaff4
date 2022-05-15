package com.github.nava2.aff4.streams.symbolics

import com.github.nava2.aff4.io.fixedLength
import okio.ByteString
import okio.Source
import org.eclipse.rdf4j.model.IRI
import java.nio.ByteBuffer

private const val BUFFER_SIZE: Int = 1 * 1024 // 1KiB

data class SymbolicSourceProvider(
  val arn: IRI,
  val pattern: ByteString,
  val chunkBoundary: Int,
) {
  private val patternBuffer: ByteBuffer

  init {
    val nearestBufferSizeWithFullPatterns = pattern.size * BUFFER_SIZE.floorDiv(pattern.size)
    check(nearestBufferSizeWithFullPatterns % pattern.size == 0)

    val byteBuffer = ByteBuffer.allocate(nearestBufferSizeWithFullPatterns)

    val patternFillBuffer = pattern.asByteBuffer()
    while (byteBuffer.hasRemaining()) {
      byteBuffer.put(patternFillBuffer)
      patternFillBuffer.rewind()
    }

    byteBuffer.rewind()
    this.patternBuffer = byteBuffer.asReadOnlyBuffer()
  }

  fun fixed(length: Long): Source = infinite().fixedLength(length)

  fun infinite(): Source = InfinitePatternSource(patternBuffer.asReadOnlyBuffer(), chunkBoundary)
}
