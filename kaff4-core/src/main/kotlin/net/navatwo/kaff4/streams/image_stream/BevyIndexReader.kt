package net.navatwo.kaff4.streams.image_stream

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import net.navatwo.kaff4.container.ContainerDataFileSystemProvider
import net.navatwo.kaff4.io.availableRange
import net.navatwo.kaff4.model.rdf.ImageStream
import okio.BufferedSource
import okio.Timeout
import okio.buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val INDEX_BUFFER_VALUE_COUNT: Int = 20

internal class BevyIndexReader @AssistedInject constructor(
  private val bevyIndexCache: BevyIndexCache,
  containerDataFileSystemProvider: ContainerDataFileSystemProvider,
  @Assisted imageStream: ImageStream,
  @Assisted private val bevy: Bevy,
) : AutoCloseable {

  private val fileSystem = containerDataFileSystemProvider.get(imageStream)

  private val chunkSize = imageStream.chunkSize

  private val bufferArray = ByteBuffer.allocate(IndexValue.SIZE_BYTES * INDEX_BUFFER_VALUE_COUNT)
    .order(ByteOrder.LITTLE_ENDIAN)
    .limit(0)

  private var position: Long = 0L
  private var currentSource: BufferedSource? = null

  fun readIndexContaining(bevyPosition: Long, timeout: Timeout): IndexValue? {
    timeout.throwIfReached()

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

  @Synchronized
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

    if (bufferArray.remaining() < IndexValue.SIZE_BYTES) {
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

  @Synchronized
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
