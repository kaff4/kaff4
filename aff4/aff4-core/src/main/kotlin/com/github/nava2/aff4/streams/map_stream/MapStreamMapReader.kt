package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.interval_tree.IntervalTree
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.rdf.MapStream
import okio.FileSystem
import okio.buffer
import org.eclipse.rdf4j.model.IRI
import javax.inject.Inject

private const val ENTRIES_INSERT_CHUNK_SIZE = 50

internal class MapStreamMapReader @Inject constructor(
  @ForImageRoot private val imageRootFileSystem: FileSystem,
  private val mapIdxFileReader: MapIdxFileReader,
) {
  fun loadMap(mapStream: MapStream): MapStreamMap {
    val index = mapIdxFileReader.loadTargets(mapStream)

    val entryTree = IntervalTree<MapStreamMap.IntervalEntry>()

    for (entries in streamEntries(mapStream, index).chunked(ENTRIES_INSERT_CHUNK_SIZE)) {
      entryTree.insertAll(entries.map { MapStreamMap.IntervalEntry(it) })
    }

    return MapStreamMap(mapStream.mapGapDefaultStream, mapStream.size, entryTree)
  }

  private fun streamEntries(
    mapStream: MapStream,
    index: List<IRI>
  ): Sequence<MapStreamEntry> = sequence {
    val mapMapFile = mapStream.mapPath

    val requiredBytes = MapStreamEntry.SIZE_BYTES.toLong()

    imageRootFileSystem.source(mapMapFile).buffer().use { source ->
      while (!source.exhausted()) {
        source.require(requiredBytes)

        val mappedOffset = source.readLongLe()
        val length = source.readLongLe()
        val targetOffset = source.readLongLe()
        val targetId = source.readIntLe()

        val targetIri = index[targetId]

        // We avoid keeping the gap stream in memory as its wasteful
        if (targetIri == mapStream.mapGapDefaultStream && targetOffset == mappedOffset) continue

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
