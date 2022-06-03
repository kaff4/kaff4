package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.interval_tree.IntervalTree
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.streams.symbolics.Symbolics
import okio.FileSystem
import okio.buffer
import org.eclipse.rdf4j.model.IRI
import javax.inject.Inject

private const val ENTRIES_INSERT_CHUNK_SIZE = 50

internal class MapStreamMapReader @Inject constructor(
  @ForImageRoot private val imageRootFileSystem: FileSystem,
  private val aff4Model: Aff4Model,
  private val mapIdxFileReader: MapIdxFileReader,
  private val symbolics: Symbolics,
) {
  fun loadMap(mapStream: MapStream): MapStreamMap {
    val index = mapIdxFileReader.loadTargets(mapStream)

    val sortedEntries = sortedSetOf<MapStreamEntry>()

    for (entries in streamEntries(mapStream, index).chunked(ENTRIES_INSERT_CHUNK_SIZE) { it.toSortedSet() }) {
      sortedEntries.addAll(entries)
    }

    val compressedTree = IntervalTree<MapStreamEntry>().apply {
      insertAll(sortedEntries.compressedSequence())
    }

    return MapStreamMap(
      gapTargetStream = mapStream.mapGapDefaultStream ?: symbolics.zero.arn,
      size = mapStream.size,
      entryTree = compressedTree,
    )
  }

  private fun streamEntries(
    mapStream: MapStream,
    index: List<IRI>
  ): Sequence<MapStreamEntry> = sequence {
    val mapMapFile = mapStream.mapPath(aff4Model.containerArn)
    val gapDefaultStream = mapStream.mapGapDefaultStream ?: symbolics.zero.arn

    imageRootFileSystem.source(mapMapFile).buffer().use { source ->
      while (!source.exhausted()) {
        source.require(MapStreamEntry.SIZE_BYTES)

        val mappedOffset = source.readLongLe()
        val length = source.readLongLe()
        val targetOffset = source.readLongLe()
        val targetId = source.readIntLe()

        val targetIri = index[targetId]

        // We avoid keeping the gap stream in memory as its wasteful
        if (targetIri == gapDefaultStream && targetOffset == mappedOffset) continue

        val entry = MapStreamEntry(
          mappedOffset = mappedOffset,
          length = length,
          targetOffset = targetOffset,
          targetIRI = targetIri,
        )
        yield(entry)
      }
    }
  }
}
