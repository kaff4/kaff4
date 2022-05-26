package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.SourceProvider
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import java.nio.ByteBuffer

internal class Aff4BevySource(
  context: Aff4BevySourceContext,
  private var position: Long,
  private val timeout: Timeout,
) : Source {

  private val compressionMethod = context.imageStream.compressionMethod
  private val chunkSize = context.imageStream.chunkSize

  private val imageBlockHashVerification = context.imageBlockHashVerification
  private val bevyIndexReader = context.bevyIndexReader
  private val bevyChunkCache = context.bevyChunkCache
  private val bevy = context.bevy

  private var lastDataSourcePosition = 0L

  private val dataSourceProvider: SourceProvider<BufferedSource> = context.dataSegmentSourceProvider()
  private var dataSource: BufferedSource? = null

  private val uncompressedSize = context.uncompressedSize

  private val chunkBuffer = ByteBuffer.allocateDirect(chunkSize)

  init {
    chunkBuffer.limit(0)
  }

  private val compressedChunkBuffer = ByteBuffer.allocateDirect(chunkSize)

  override fun read(sink: Buffer, byteCount: Long): Long {
    require(byteCount >= 0L)

    timeout.throwIfReached()

    if (position == uncompressedSize) return -1

    val maxBytesToRead = byteCount.coerceAtMost(uncompressedSize - position)

    if (!chunkBuffer.hasRemaining()) {
      readIntoBuffer()
    }

    val readSlice = chunkBuffer.slice(
      chunkBuffer.position(),
      maxBytesToRead.toInt().coerceAtMost(chunkBuffer.remaining()),
    )

    val readIntoSink = sink.write(readSlice)
    chunkBuffer.position(chunkBuffer.position() + readIntoSink)

    position += readIntoSink
    return readIntoSink.toLong()
  }

  override fun close() {
    dataSource?.close()
    lastDataSourcePosition = 0
  }

  override fun timeout(): Timeout = timeout

  private fun readIntoBuffer() {
    val index = bevyIndexReader.readIndexContaining(position, timeout) ?: return
    check(index.compressedLength <= chunkSize) {
      "Read invalid compressed chunk index.length: ${index.compressedLength}"
    }

    timeout.throwIfReached()

    chunkBuffer.rewind()

    bevyChunkCache.getOrPutInto(bevy, index, chunkBuffer) {
      readCompressedBuffer(timeout, index.dataPosition, index.compressedLength)

      val chunkBufferLength = compressionMethod.uncompress(compressedChunkBuffer, chunkBuffer)

      chunkBuffer.limit(chunkBufferLength)

      if (chunkBufferLength <= index.compressedLength) {
        // data wasn't compressed, so 1-1 copy it
        chunkBuffer.rewind()
        chunkBuffer.limit(compressedChunkBuffer.limit())
        chunkBuffer.put(compressedChunkBuffer)
      }

      chunkBuffer.rewind()

      imageBlockHashVerification.verifyBlock(bevy, position.floorDiv(chunkSize), chunkBuffer, timeout)
    }

    chunkBuffer.position((position % chunkSize).toInt())
  }

  private fun readCompressedBuffer(timeout: Timeout, dataPosition: Long, byteCount: Int) {
    if (dataSource == null || dataPosition < lastDataSourcePosition) {
      dataSource?.close()
      dataSource = dataSourceProvider.source(timeout)
      lastDataSourcePosition = 0
    }

    dataSource!!.apply {
      skip(dataPosition - lastDataSourcePosition)
      lastDataSourcePosition = dataPosition

      compressedChunkBuffer.rewind()
      compressedChunkBuffer.limit(byteCount)

      while (compressedChunkBuffer.hasRemaining()) {
        val dataRead = read(compressedChunkBuffer)
        lastDataSourcePosition += dataRead

        if (dataRead == 0) break
      }
    }

    // check after such that `lastDataSourcePosition is always correct
    check(!compressedChunkBuffer.hasRemaining())

    compressedChunkBuffer.rewind()
  }
}
