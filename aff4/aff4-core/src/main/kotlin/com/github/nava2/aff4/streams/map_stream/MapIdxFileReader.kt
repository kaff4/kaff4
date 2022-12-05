package com.github.nava2.aff4.streams.map_stream

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.nava2.aff4.container.ContainerDataFileSystemProvider
import com.github.nava2.aff4.io.lineSequence
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.model.rdf.createArn
import okio.Path
import okio.buffer
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject
import javax.inject.Singleton

private const val MAP_TARGETS_CACHE_SIZE = 10L

@Singleton
internal class MapIdxFileReader @Inject constructor(
  private val containerDataFileSystemProvider: ContainerDataFileSystemProvider,
  private val valueFactory: ValueFactory,
) {
  private val targetsCache = Caffeine.newBuilder()
    .maximumSize(MAP_TARGETS_CACHE_SIZE)
    .build<CacheKey, List<Aff4Arn>> { key ->
      containerDataFileSystemProvider[key.stored]
        .source(key.mapIdxPath).buffer().use { s ->
          s.lineSequence().map { valueFactory.createArn(it) }.toList()
        }
    }

  fun loadTargets(mapStream: MapStream): List<Aff4Arn> {
    val cacheKey = CacheKey(mapStream.stored, mapStream.idxPath)
    return targetsCache.get(cacheKey)!!
  }

  private data class CacheKey(
    val stored: Aff4Arn,
    val mapIdxPath: Path,
  )
}
