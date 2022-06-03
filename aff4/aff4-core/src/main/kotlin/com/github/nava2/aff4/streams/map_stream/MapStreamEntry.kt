package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.interval_tree.Interval
import okio.BufferedSink
import org.eclipse.rdf4j.model.IRI

data class MapStreamEntry(
  val mappedOffset: Long,
  override val length: Long,
  val targetOffset: Long,
  val targetIRI: IRI,
) : Interval {

  override val start: Long = mappedOffset
  override val end: Long = mappedOffset + length

  val mappedEndOffset: Long = end

  fun canMerge(other: MapStreamEntry): Boolean {
    if (!(isAdjacentOrOverlaps(other) && targetIRI == other.targetIRI)) return false

    // If these values are not the same then the order of the two is not consistent or they are not identically
    // sequential in both mapped spaces
    val mappedDiff = mappedOffset - other.mappedOffset
    val targetDiff = targetOffset - other.targetOffset
    return mappedDiff == targetDiff
  }

  fun merge(other: MapStreamEntry): MapStreamEntry {
    require(canMerge(other)) { "$this and $other are not mergable" }

    val newOffsetInterval = super.merge(other)

    return MapStreamEntry(
      mappedOffset = newOffsetInterval.start,
      length = newOffsetInterval.length,
      targetOffset = minOf(targetOffset, other.targetOffset),
      targetIRI = targetIRI,
    )
  }

  fun writeToSink(targetMap: Map<IRI, Int>, sink: BufferedSink) {
    val targetIndex = targetMap.getValue(targetIRI)

    sink.writeLongLe(mappedOffset)
    sink.writeLongLe(length)
    sink.writeLongLe(targetOffset)
    sink.writeIntLe(targetIndex)
  }

  companion object {
    const val SIZE_BYTES: Long = ULong.SIZE_BYTES.toLong() + ULong.SIZE_BYTES + ULong.SIZE_BYTES + UInt.SIZE_BYTES
  }
}
