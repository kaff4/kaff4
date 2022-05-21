package com.github.nava2.aff4.streams.symbolics

import com.github.nava2.aff4.io.fixedLength
import com.github.nava2.aff4.model.Aff4Stream
import okio.ByteString
import okio.Source
import org.eclipse.rdf4j.model.IRI
import java.nio.ByteBuffer

private const val BUFFER_SIZE: Int = 16 * 1024 // 16KiB

data class SymbolicSourceProvider(
  val arn: IRI,
  val pattern: ByteString,
  val chunkBoundary: Int,
) : Aff4Stream {
  override val size: Long = Long.MAX_VALUE

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

  override fun source(position: Long): Source = infinite()

  override fun close() = Unit

  fun fixed(length: Long): Source = infinite().fixedLength(length)

  fun infinite(): Source = InfinitePatternSource(pattern, patternBuffer.asReadOnlyBuffer(), chunkBoundary)
}
