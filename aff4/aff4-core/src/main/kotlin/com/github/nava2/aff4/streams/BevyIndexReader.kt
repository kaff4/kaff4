package com.github.nava2.aff4.streams

import com.github.nava2.aff4.meta.rdf.model.ImageStream
import okio.BufferedSource
import okio.FileSystem
import okio.buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class BevyIndexReader(
  private val fileSystem: FileSystem,
  imageStream: ImageStream,
  private val config: Bevy,
) : AutoCloseable {
  private val bevyIndexCache = mutableMapOf<Long, IndexValue?>()

  private val chunkSize = imageStream.chunkSize

  private val bufferArray = ByteBuffer.allocate(IndexValue.SIZE_BYTES)
  private val offsetBuffer = bufferArray.slice(0, ULong.SIZE_BYTES)
    .order(ByteOrder.LITTLE_ENDIAN)
    .asLongBuffer()
    .asReadOnlyBuffer()

  private val lengthBuffer = bufferArray.slice(ULong.SIZE_BYTES, UInt.SIZE_BYTES)
    .order(ByteOrder.LITTLE_ENDIAN)
    .asIntBuffer()
    .asReadOnlyBuffer()

  private val indexFileSize = fileSystem.metadata(config.indexSegment).size!!

  private val lastIndexIndex = indexFileSize / IndexValue.SIZE_BYTES - 1
  private val lastIndexPosition = (lastIndexIndex + 1) * chunkSize - 1

  fun readIndexContaining(bevyPosition: Long): IndexValue? {
    require(bevyPosition >= 0L) { "bevyPosition must be positive" }

    if (bevyPosition !in 0L..lastIndexPosition) return null

    // round down by chunk size to find the index to read
    val indexIndex = bevyPosition.floorDiv(chunkSize)

    return bevyIndexCache.getOrPut(indexIndex) {
      val indexFilePosition = indexIndex * IndexValue.SIZE_BYTES
      val readResult = readIndexFile(indexFilePosition)

      val offset = offsetBuffer.get(0)
      val length = lengthBuffer.get(0)
      IndexValue(offset, length).takeIf { readResult == IndexValue.SIZE_BYTES }
    }
  }

  @Synchronized
  override fun close() {
    bufferedSource?.close()
    bufferedSource = null
  }

  private var lastReadPosition = 0L
  private var bufferedSource: BufferedSource? = null

  @Synchronized
  private fun readIndexFile(position: Long): Int {
    if (bufferedSource == null || lastReadPosition < 0 || lastReadPosition > position) {
      // back tracking, which is fine but need to get a new stream
      bufferedSource?.close()

      bufferedSource = fileSystem.source(config.indexSegment).buffer()
      lastReadPosition = 0
    }

    return bufferedSource!!.run {
      skip(position - lastReadPosition)

      lastReadPosition = position + bufferArray.limit()
      read(bufferArray.array())
    }
  }
}

internal data class IndexValue(
  val bevyPosition: Long,
  val compressedLength: Int,
) {
  companion object {
    const val SIZE_BYTES = ULong.SIZE_BYTES + UInt.SIZE_BYTES
  }
}
