package com.github.nava2.aff4.streams.image_stream

import com.github.benmanes.caffeine.cache.Caffeine
import org.eclipse.rdf4j.model.IRI
import java.nio.ByteBuffer

private const val CHUNK_CACHE_SIZE = 50L

internal class BevyChunkCache(
  private val chunkSize: Int,
) {
  private val chunkCache = Caffeine.newBuilder()
    .maximumSize(CHUNK_CACHE_SIZE)
    .recordStats()
    .build<CacheKey, CacheValue>()

  fun stats() = chunkCache.stats()

  fun getOrPutInto(
    bevy: Bevy,
    indexValue: IndexValue,
    uncompressedBuffer: ByteBuffer,
    readUncompressed: () -> Unit
  ) {
    val cacheKey = CacheKey(bevy.arn, indexValue)

    var wasCached = true
    val cachedData = chunkCache.get(cacheKey) {
      wasCached = false

      readUncompressed()

      val cacheValue = CacheValue(ByteArray(chunkSize))

      uncompressedBuffer.mark()
      ByteBuffer.wrap(cacheValue.uncompressedData).put(uncompressedBuffer)
      uncompressedBuffer.reset()

      cacheValue
    }

    // when we need to load, the buffer is already setup
    if (!wasCached) return

    uncompressedBuffer.mark()

    uncompressedBuffer.limit(chunkSize)
    uncompressedBuffer.put(cachedData.uncompressedData)

    uncompressedBuffer.reset()
  }

  private data class CacheKey(
    val bevyArn: IRI,
    val indexValue: IndexValue,
  )

  private class CacheValue(
    val uncompressedData: ByteArray,
  )
}
