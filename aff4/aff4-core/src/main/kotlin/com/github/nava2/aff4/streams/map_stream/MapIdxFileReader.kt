package com.github.nava2.aff4.streams.map_stream

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.nava2.aff4.io.lineSequence
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.model.rdf.createArn
import okio.FileSystem
import okio.Path
import okio.buffer
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject

private const val MAP_TARGETS_CACHE_SIZE = 10L

internal class MapIdxFileReader @Inject constructor(
  @ForImageRoot private val imageRootFileSystem: FileSystem,
  private val aff4Model: Aff4Model,
  private val valueFactory: ValueFactory,
) {
  private val targetsCache = Caffeine.newBuilder()
    .maximumSize(MAP_TARGETS_CACHE_SIZE)
    .build<CacheKey, List<Aff4Arn>> { key ->
      imageRootFileSystem.source(key.mapIdxPath).buffer().use { s ->
        s.lineSequence().map { valueFactory.createArn(it) }.toList()
      }
    }

  fun loadTargets(mapStream: MapStream): List<Aff4Arn> {
    val cacheKey = CacheKey(aff4Model.containerArn, mapStream.idxPath(aff4Model.containerArn))
    return targetsCache.get(cacheKey)!!
  }

  private data class CacheKey(
    val containerArn: Aff4Arn,
    val mapIdxPath: Path,
  )
}
