package com.github.nava2.aff4.streams.map_stream

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.nava2.aff4.io.lineSequence
import com.github.nava2.aff4.meta.rdf.model.MapStream
import com.github.nava2.aff4.meta.rdf.parser.ForImageRoot
import com.github.nava2.aff4.meta.rdf.toAff4Path
import okio.FileSystem
import okio.buffer
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject

private const val MAP_TARGETS_CACHE_SIZE = 10L

internal class MapIdxFileReader @Inject constructor(
  @ForImageRoot private val imageRootFileSystem: FileSystem,
  private val valueFactory: ValueFactory,
) {
  private val targetsCache = Caffeine.newBuilder()
    .maximumSize(MAP_TARGETS_CACHE_SIZE)
    .build<IRI, List<IRI>> { mapStreamArn ->
      val targetFile = mapStreamArn.toAff4Path() / "idx"
      imageRootFileSystem.source(targetFile).buffer().use { s ->
        s.lineSequence().map { valueFactory.createIRI(it) }.toList()
      }
    }

  fun loadTargets(mapStream: MapStream): List<IRI> = targetsCache.get(mapStream.arn)!!
}
