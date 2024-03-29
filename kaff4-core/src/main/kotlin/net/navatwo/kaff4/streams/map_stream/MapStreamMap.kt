package net.navatwo.kaff4.streams.map_stream

import com.github.nava2.interval_tree.Interval
import com.github.nava2.interval_tree.IntervalTree
import com.google.common.base.MoreObjects
import org.eclipse.rdf4j.model.IRI
import java.util.SortedSet

internal class MapStreamMap(
  private val gapTargetStream: IRI,
  private val size: Long,
  private val entryTree: IntervalTree<MapStreamEntry>,
) {

  private val entrySet: SortedSet<MapStreamEntry> = entryTree.toSortedSet()

  /**
   * Queries the set of map stream entries that cover the requested interval.
   *
   * Gaps are populated as needed and the result sequence is always contiguous.
   */
  fun query(mappedOffset: Long, length: Long): Sequence<MapStreamEntry> = sequence {
    check(mappedOffset >= 0)
    check(length >= 0)

    val treeEntries = entryTree.overlappers(mappedOffset, length)

    val finalOffset = (mappedOffset + length).coerceAtMost(size)

    var prevEntry: MapStreamEntry? = null
    for (entry in treeEntries) {
      // Check for synthetic gaps
      if (prevEntry == null) {
        if (entry.mappedOffset > mappedOffset) {
          // Requested a gap for first read
          yield(
            generateGapEntry(mappedOffset, length = entry.mappedOffset - mappedOffset)
          )
        }
      } else if (prevEntry.mappedEndOffset != entry.mappedOffset) {
        yield(
          generateGapEntry(
            mappedOffset = prevEntry.mappedEndOffset,
            length = entry.mappedOffset - prevEntry.mappedEndOffset
          )
        )
      }

      yield(entry.truncateEntry())
      prevEntry = entry
    }

    // special case where there were _no_ matching entries so the loop never iterated
    val nextEntryStart by lazy(LazyThreadSafetyMode.NONE) {
      val checkInterval = prevEntry ?: Interval.Simple(mappedOffset, 0)
      entryTree.closestSuccessor(checkInterval)
        .map { it.start }
        .orElse(finalOffset)
    }

    if (prevEntry == null) {
      yield(
        generateGapEntry(mappedOffset, nextEntryStart - mappedOffset)
      )
    } else if (prevEntry.mappedEndOffset != finalOffset) {
      // Gap at the end of the request
      yield(
        generateGapEntry(prevEntry.mappedEndOffset, nextEntryStart - prevEntry.mappedEndOffset)
      )
    }
  }

  private fun generateGapEntry(mappedOffset: Long, length: Long): MapStreamEntry {
    return MapStreamEntry(
      mappedOffset = mappedOffset,
      length = length,
      targetOffset = mappedOffset,
      targetArn = gapTargetStream,
    ).truncateEntry()
  }

  private fun MapStreamEntry.truncateEntry(): MapStreamEntry {
    return copy(length = length.coerceAtMost(size - mappedOffset))
  }

  override fun toString(): String {
    return MoreObjects.toStringHelper(this)
      .add("entries", entrySet)
      .toString()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MapStreamMap) return false

    return entrySet == other.entrySet
  }

  override fun hashCode(): Int {
    return entrySet.hashCode()
  }

  data class IntervalEntry(val entry: MapStreamEntry) : Interval {
    override val start: Long = entry.mappedOffset
    override val endExclusive: Long = entry.mappedOffset + entry.length
    override val length: Long = entry.length
  }
}
