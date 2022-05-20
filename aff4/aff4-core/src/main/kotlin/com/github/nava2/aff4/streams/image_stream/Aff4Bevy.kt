package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.sourceProvider
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.meta.rdf.model.ImageStream
import com.github.nava2.aff4.streams.SourceProviderWithRefCounts
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Source
import java.nio.ByteBuffer

internal class Aff4Bevy @AssistedInject constructor(
  private val imageBlockHashVerification: ImageBlockHashVerification,
  @ForImageRoot fileSystem: FileSystem,
  @Assisted imageStreamConfig: ImageStream,
  @Assisted private val bevyIndexReader: BevyIndexReader,
  @Assisted private val bevyChunkCache: BevyChunkCache,
  @Assisted val bevy: Bevy,
) : AutoCloseable {
  private val sourceProviderWithRefCounts = SourceProviderWithRefCounts(::readAt)

  private val compressionMethod = imageStreamConfig.compressionMethod
  private val chunkSize = imageStreamConfig.chunkSize

  private var position: Long = 0L

  private val chunkBuffer = ByteBuffer.allocateDirect(chunkSize)

  init {
    chunkBuffer.limit(0)
  }

  private val compressedChunkBuffer = ByteBuffer.allocateDirect(chunkSize)

  private var lastDataSourcePosition = 0L

  val uncompressedSize = imageStreamConfig.bevySize(bevy.index)

  private val dataSourceProvider = fileSystem.sourceProvider(bevy.dataSegment).buffer()
  private var dataSource: BufferedSource? = null

  fun source(position: Long): Source {
    return sourceProviderWithRefCounts.source(position)
  }

  override fun close() {
    sourceProviderWithRefCounts.close()
    bevyIndexReader.close()

    dataSource?.close()
    lastDataSourcePosition = 0
  }

  private fun readAt(readPosition: Long, sink: Buffer, byteCount: Long): Long {
    moveTo(readPosition)

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

  private fun moveTo(newPosition: Long) {
    if (position == newPosition) return

    val currentChunkIndex = (position - chunkBuffer.remaining()).floorDiv(chunkSize)
    val newChunkIndex = newPosition.floorDiv(chunkSize)

    val newPositionInChunk = (newPosition - newChunkIndex * chunkSize).toInt()
    if (newChunkIndex == currentChunkIndex) {
      chunkBuffer.position(newPositionInChunk)
    } else {
      // set our selves to be "zeroed"
      chunkBuffer.limit(0)
    }

    position = newPosition
  }

  private fun readIntoBuffer() {
    val index = bevyIndexReader.readIndexContaining(position) ?: return
    check(index.compressedLength <= chunkSize) {
      "Read invalid compressed chunk index.length: ${index.compressedLength}"
    }

    chunkBuffer.rewind()

    bevyChunkCache.getOrPutInto(bevy, index, chunkBuffer) {
      readCompressedBuffer(index.dataPosition, index.compressedLength)

      val chunkBufferLength = compressionMethod.uncompress(compressedChunkBuffer, chunkBuffer)

      chunkBuffer.limit(chunkBufferLength)

      if (chunkBufferLength <= index.compressedLength) {
        // data wasn't compressed, so 1-1 copy it
        chunkBuffer.rewind()
        chunkBuffer.limit(compressedChunkBuffer.limit())
        chunkBuffer.put(compressedChunkBuffer)
      }

      chunkBuffer.rewind()

      imageBlockHashVerification.verifyBlock(bevy, position.floorDiv(chunkSize), chunkBuffer)
    }

    chunkBuffer.position((position % chunkSize).toInt())
  }

  private fun readCompressedBuffer(dataPosition: Long, byteCount: Int) {
    if (dataSource == null || dataPosition < lastDataSourcePosition) {
      dataSource?.close()
      dataSource = dataSourceProvider.get()
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

  interface AssistedFactory {
    fun create(
      imageStreamConfig: ImageStream,
      bevyIndexReader: BevyIndexReader,
      bevyChunkCache: BevyChunkCache,
      bevy: Bevy,
    ): Aff4Bevy
  }
}
