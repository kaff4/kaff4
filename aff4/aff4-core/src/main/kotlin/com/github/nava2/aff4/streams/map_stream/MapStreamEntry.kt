package com.github.nava2.aff4.streams.map_stream

import org.eclipse.rdf4j.model.IRI

data class MapStreamEntry(
  val mappedOffset: Long,
  val length: Long,
  val targetOffset: Long,
  val targetIRI: IRI,
) : Comparable<MapStreamEntry> {

  val mappedEndOffset: Long = mappedOffset + length

  override fun compareTo(other: MapStreamEntry): Int = COMPARATOR.compare(this, other)

  companion object {
    const val SIZE_BYTES = ULong.SIZE_BYTES + ULong.SIZE_BYTES + ULong.SIZE_BYTES + UInt.SIZE_BYTES

    private val COMPARATOR: Comparator<MapStreamEntry> =
      Comparator.comparing<MapStreamEntry, Long> { it.mappedOffset }
        .thenComparing<Long> { it.length }
  }
}
