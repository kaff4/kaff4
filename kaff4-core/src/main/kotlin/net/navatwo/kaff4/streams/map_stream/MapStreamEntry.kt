package net.navatwo.kaff4.streams.map_stream

import com.github.nava2.interval_tree.Interval
import net.navatwo.kaff4.model.rdf.Aff4Arn
import okio.BufferedSink

internal data class MapStreamEntry(
  val mappedOffset: Long,
  override val length: Long,
  val targetOffset: Long,
  val targetArn: Aff4Arn,
) : Interval {

  override val start: Long = mappedOffset
  override val endExclusive: Long = mappedOffset + length

  val mappedEndOffset: Long = endExclusive

  fun canMerge(other: MapStreamEntry): Boolean {
    if (!(isAdjacentOrOverlaps(other) && targetArn == other.targetArn)) return false

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
      targetArn = targetArn,
    )
  }

  fun writeToSink(targetMap: Map<Aff4Arn, Int>, sink: BufferedSink) {
    val targetIndex = targetMap.getValue(targetArn)

    sink.writeLongLe(mappedOffset)
    sink.writeLongLe(length)
    sink.writeLongLe(targetOffset)
    sink.writeIntLe(targetIndex)
  }

  companion object {
    const val SIZE_BYTES: Long = ULong.SIZE_BYTES.toLong() + ULong.SIZE_BYTES + ULong.SIZE_BYTES + UInt.SIZE_BYTES
  }
}
