package net.navatwo.kaff4.streams.map_stream

import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.navatwo.kaff4.container.ContainerDataFileSystemProvider
import net.navatwo.kaff4.io.BufferedSource
import net.navatwo.kaff4.io.asKAff4
import net.navatwo.kaff4.io.buffer
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.model.rdf.createArn
import okio.Path
import org.eclipse.rdf4j.model.ValueFactory

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
        .source(key.mapIdxPath)
        .asKAff4()
        .buffer()
        .use { s ->
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

private fun BufferedSource.lineSequence(): Sequence<String> = sequence {
  var targetLine = readUtf8Line()
  while (targetLine != null) {
    yield(targetLine)
    targetLine = readUtf8Line()
  }
}

private fun BufferedSource.readUtf8Line(): String? = asOkio().readUtf8Line()
