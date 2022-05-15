package com.github.nava2.aff4.streams

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.nava2.aff4.meta.rdf.model.ImageStream
import okio.BufferedSource
import okio.FileSystem
import okio.buffer
import org.eclipse.rdf4j.model.IRI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration

private const val INDEX_CACHE_SIZE = 500L
private const val SOURCE_CACHE_SIZE = 5L

internal class BevyIndexReader(
  private val fileSystem: FileSystem,
  imageStream: ImageStream,
) : AutoCloseable {
  private val bevyIndexCache = Caffeine.newBuilder()
    .maximumSize(INDEX_CACHE_SIZE)
    .build<CacheKey, CacheValue>()

  private val bevyIndexSourceCache = Caffeine.newBuilder()
    .maximumSize(SOURCE_CACHE_SIZE)
    .evictionListener<Bevy, BufferedSourceInfo> { _, value, removalCause ->
      if (removalCause != RemovalCause.REPLACED) {
        value?.close()
      }
    }
    .expireAfterWrite(Duration.ofMinutes(1))
    .build<Bevy, BufferedSourceInfo> { BufferedSourceInfo(fileSystem.source(it.indexSegment).buffer(), 0) }

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

  fun readIndexContaining(bevy: Bevy, bevyPosition: Long): IndexValue? {
    require(bevyPosition >= 0L) { "bevyPosition must be positive" }

    // round down by chunk size to find the index to read
    val indexIndex = bevyPosition.floorDiv(chunkSize)

    return bevyIndexCache.get(CacheKey(bevy.arn, indexIndex)) {
      loadIndex(bevy, indexIndex)
    }.indexValue
  }

  override fun close() {
    bevyIndexSourceCache.invalidateAll()
    bevyIndexSourceCache.cleanUp()
  }

  private fun loadIndex(bevy: Bevy, indexIndex: Long): CacheValue {
    val indexFilePosition = indexIndex * IndexValue.SIZE_BYTES
    val readResult = readIndexFile(bevy, indexFilePosition)

    return if (readResult == IndexValue.SIZE_BYTES) {
      val offset = offsetBuffer.get(0)
      val length = lengthBuffer.get(0)
      val result = IndexValue(offset, length).takeIf { readResult == IndexValue.SIZE_BYTES }
      CacheValue(result)
    } else {
      CacheValue.NULL_INSTANCE
    }
  }

  @Synchronized
  private fun readIndexFile(bevy: Bevy, position: Long): Int {
    val bufferedSourceInfo = bevyIndexSourceCache.get(bevy)

    var bufferedSource = bufferedSourceInfo.source
    var lastReadPosition = bufferedSourceInfo.position

    if (lastReadPosition < 0 || lastReadPosition > position) {
      // back tracking, which is fine but need to get a new stream
      bufferedSource.close()

      bufferedSource = fileSystem.source(bevy.indexSegment).buffer()
      lastReadPosition = 0
    }

    val bytesRead = bufferedSource.run {
      skip(position - lastReadPosition)

      lastReadPosition = position + bufferArray.limit()
      read(bufferArray.array())
    }

    bevyIndexSourceCache.put(bevy, bufferedSourceInfo.copy(source = bufferedSource, position = lastReadPosition))

    return bytesRead
  }

  private data class CacheKey(
    val bevyArn: IRI,
    val indexIndex: Long,
  )

  private data class CacheValue(val indexValue: IndexValue?) {
    companion object {
      val NULL_INSTANCE = CacheValue(null)
    }
  }

  private data class BufferedSourceInfo(
    val source: BufferedSource,
    val position: Long,
  ) : AutoCloseable by source
}

internal data class IndexValue(
  val dataPosition: Long,
  val compressedLength: Int,
) {
  companion object {
    const val SIZE_BYTES = ULong.SIZE_BYTES + UInt.SIZE_BYTES
  }
}
