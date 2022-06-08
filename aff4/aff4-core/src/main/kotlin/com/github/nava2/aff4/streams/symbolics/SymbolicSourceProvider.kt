package com.github.nava2.aff4.streams.symbolics

import com.github.nava2.aff4.model.Aff4StreamSourceProvider
import okio.ByteString
import okio.Source
import okio.Timeout
import org.eclipse.rdf4j.model.IRI
import java.nio.ByteBuffer

private const val BUFFER_SIZE: Int = 16 * 1024 // 16KiB

data class SymbolicSourceProvider(
  override val arn: IRI,
  val pattern: ByteString,
  val chunkBoundary: Int,
) : Aff4StreamSourceProvider {
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

  override fun source(position: Long, timeout: Timeout): Source = InfinitePatternSource(
    pattern = pattern,
    patternBuffer = patternBuffer.asReadOnlyBuffer(),
    repetitionBoundary = chunkBoundary,
    timeout = timeout,
  )
}
