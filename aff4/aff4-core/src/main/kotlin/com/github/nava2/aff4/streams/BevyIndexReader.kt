package com.github.nava2.aff4.streams

import com.github.nava2.aff4.io.availableRange
import com.github.nava2.aff4.meta.rdf.model.ImageStream
import com.github.nava2.aff4.meta.rdf.parser.ForImageRoot
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.BufferedSource
import okio.FileSystem
import okio.buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val INDEX_BUFFER_VALUE_COUNT: Int = 20

internal class BevyIndexReader @AssistedInject constructor(
  private val bevyIndexCache: BevyIndexCache,
  @ForImageRoot private val fileSystem: FileSystem,
  @Assisted imageStream: ImageStream,
  @Assisted private val bevy: Bevy,
) : AutoCloseable {

  private val chunkSize = imageStream.chunkSize

  private val bufferArray = ByteBuffer.allocate(IndexValue.SIZE_BYTES * INDEX_BUFFER_VALUE_COUNT)
    .order(ByteOrder.LITTLE_ENDIAN)
    .limit(0)

  private var position: Long = 0L
  private var currentSource: BufferedSource? = null

  fun readIndexContaining(bevyPosition: Long): IndexValue? {
    require(bevyPosition >= 0L) { "bevyPosition must be positive" }

    // round down by chunk size to find the index to read
    val indexIndex = bevyPosition.floorDiv(chunkSize)

    return bevyIndexCache.getOrLoad(bevy, indexIndex) {
      loadIndex(bevy, indexIndex)
    }
  }

  override fun close() {
    resetSource()
  }

  private fun loadIndex(bevy: Bevy, indexIndex: Long): IndexValue? {
    val indexFilePosition = indexIndex * IndexValue.SIZE_BYTES

    if (indexFilePosition in bufferArray.availableRange(position)) {
      if (position != indexFilePosition) {
        bufferArray.position(bufferArray.position() + (indexFilePosition - position).toInt())
        position = indexFilePosition
      }
    } else {
      resetSource()
      position = indexFilePosition
    }

    if (!bufferArray.hasRemaining()) {
      val source = currentSource ?: run {
        val nextSource = fileSystem.source(bevy.indexSegment).buffer()
        currentSource = nextSource

        nextSource.skip(position)
        nextSource
      }

      bufferArray.limit(bufferArray.capacity())
      val bytesRead = source.read(bufferArray)
      bufferArray.rewind()
      bufferArray.limit(bytesRead)

      if (bytesRead == 0) return null
    }

    val offset = bufferArray.long
    val length = bufferArray.int

    position += IndexValue.SIZE_BYTES
    return IndexValue(offset, length)
  }

  private fun resetSource() {
    bufferArray.limit(0)
    currentSource?.close()
    currentSource = null
  }

  interface Factory {
    fun create(imageStream: ImageStream, bevy: Bevy): BevyIndexReader
  }
}

internal data class IndexValue(
  val dataPosition: Long,
  val compressedLength: Int,
) {
  companion object {
    const val SIZE_BYTES = ULong.SIZE_BYTES + UInt.SIZE_BYTES
  }
}
