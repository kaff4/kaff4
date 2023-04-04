package net.navatwo.kaff4.streams.map_stream

import com.github.nava2.interval_tree.IntervalTree
import net.navatwo.kaff4.yieldNotNull
import java.util.SortedSet

private fun Sequence<MapStreamEntry>.compressed(requireSort: Boolean): Sequence<MapStreamEntry> = sequence {
  val sortedEntries = if (requireSort) this@compressed.sorted() else this@compressed

  var previousEntry: MapStreamEntry? = null
  for (mapStreamEntry in sortedEntries) {
    if (previousEntry == null) {
      previousEntry = mapStreamEntry
      continue
    }

    previousEntry = if (previousEntry.canMerge(mapStreamEntry)) {
      previousEntry.merge(mapStreamEntry)
    } else {
      yield(previousEntry)
      mapStreamEntry
    }
  }

  yieldNotNull(previousEntry)
}

internal fun IntervalTree<MapStreamEntry>.compressedSequence(): Sequence<MapStreamEntry> {
  if (this.size <= 1) return this.asSequence()

  return asSequence().compressed(requireSort = false)
}

internal fun SortedSet<MapStreamEntry>.compressedSequence(): Sequence<MapStreamEntry> {
  return asSequence().compressed(requireSort = false)
}

internal fun Iterator<MapStreamEntry>.compressedSequence(isSorted: Boolean): Sequence<MapStreamEntry> {
  return asSequence().compressed(requireSort = !isSorted)
}
