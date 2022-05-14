package com.github.nava2.aff4.streams

import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.sourceProvider
import com.github.nava2.aff4.meta.rdf.model.ImageStream
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Source
import okio.Timeout
import java.nio.ByteBuffer
import kotlin.math.min

internal class Aff4Bevy(
  fileSystem: FileSystem,
  imageStreamConfig: ImageStream,
  private val bevyIndexReader: BevyIndexReader,
  private val bevyChunkCache: BevyChunkCache,
  private val bevy: Bevy,
) : AutoCloseable {
  private val compressionMethod = imageStreamConfig.compressionMethod
  private val chunkSize = imageStreamConfig.chunkSize

  private var position: Long = 0L

  private val chunkBuffer = ByteBuffer.allocateDirect(chunkSize)

  private var sourcesOutstanding: Long = 0

  init {
    chunkBuffer.limit(0)
  }

  private val compressedChunkBuffer = ByteBuffer.allocateDirect(chunkSize)

  private var lastDataSourcePosition = 0L
  private val dataSourceProvider = fileSystem.sourceProvider(bevy.dataSegment).buffer()
  private var dataSource: BufferedSource? = null

  @Synchronized
  fun source(position: Long): Source {
    require(position >= 0)

    sourcesOutstanding += 1
    return BevySource(this, position)
  }

  @Synchronized
  override fun close() {
    check(sourcesOutstanding == 0L) {
      "Sources were created and not freed: $sourcesOutstanding"
    }

    dataSource?.close()
    lastDataSourcePosition = 0
  }

  private class BevySource(
    private val aff4Bevy: Aff4Bevy,
    private var sourcePosition: Long,
  ) : Source {
    private var closed = false

    override fun close() {
      synchronized(aff4Bevy) {
        if (closed) return
        closed = true
        aff4Bevy.sourcesOutstanding -= 1
      }
    }

    override fun read(sink: Buffer, byteCount: Long): Long {
      check(!closed)

      val bytesRead = aff4Bevy.readAt(sourcePosition, sink, byteCount)
      sourcePosition += bytesRead
      return bytesRead
    }

    override fun timeout(): Timeout = Timeout.NONE
  }

  private fun readAt(readPosition: Long, sink: Buffer, byteCount: Long): Long {
    if (byteCount == 0L) return 0

    moveTo(readPosition)

    var remainingBytes = byteCount
    do {
      if (!chunkBuffer.hasRemaining()) {
        readIntoBuffer()

        if (!chunkBuffer.hasRemaining()) break
      }

      val readSlice = chunkBuffer.slice(chunkBuffer.position(), min(remainingBytes.toInt(), chunkBuffer.remaining()))

      val readIntoSink = sink.write(readSlice)
      chunkBuffer.position(chunkBuffer.position() + readIntoSink)

      remainingBytes -= readIntoSink
      position += readIntoSink
    } while (remainingBytes > 0 && readIntoSink > 0)

    return byteCount - remainingBytes
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
      chunkBuffer.position(chunkBuffer.limit())
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
        chunkBuffer.put(compressedChunkBuffer)
      }

      chunkBuffer.rewind()
    }
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
}
