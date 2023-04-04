package net.navatwo.kaff4.streams.map_stream

import com.github.nava2.interval_tree.IntervalTree
import net.navatwo.kaff4.container.ContainerDataFileSystemProvider
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.streams.symbolics.Symbolics
import okio.buffer
import org.eclipse.rdf4j.model.IRI
import javax.inject.Inject
import javax.inject.Singleton

private const val ENTRIES_INSERT_CHUNK_SIZE = 50

@Singleton
internal class MapStreamMapReader @Inject constructor(
  private val containerDataFileSystemProvider: ContainerDataFileSystemProvider,
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
    val mapMapFile = mapStream.mapPath
    val gapDefaultStream = mapStream.mapGapDefaultStream ?: symbolics.zero.arn

    val mapStreamVolumeFileSystem = containerDataFileSystemProvider[mapStream.stored]
    mapStreamVolumeFileSystem.source(mapMapFile).buffer().use { source ->
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
          targetArn = targetIri,
        )
        yield(entry)
      }
    }
  }
}
