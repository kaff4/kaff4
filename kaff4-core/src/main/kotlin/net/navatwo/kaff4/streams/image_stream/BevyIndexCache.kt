package net.navatwo.kaff4.streams.image_stream

import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.eclipse.rdf4j.model.IRI

private const val INDEX_CACHE_SIZE = 500L

@Singleton
internal class BevyIndexCache @Inject constructor() {
  private val cache = Caffeine.newBuilder()
    .maximumSize(INDEX_CACHE_SIZE)
    .build<CacheKey, CacheValue>()

  fun getOrLoad(bevy: Bevy, indexIndex: Long, loader: () -> IndexValue?): IndexValue? {
    val cacheKey = CacheKey(bevy.arn, indexIndex)
    val cacheValue = cache.get(cacheKey) {
      loader()
        ?.let { CacheValue(it) }
        ?: CacheValue.NULL_INSTANCE
    }

    return cacheValue.indexValue
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
}
