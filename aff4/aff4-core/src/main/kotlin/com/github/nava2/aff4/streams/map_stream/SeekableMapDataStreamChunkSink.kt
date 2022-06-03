package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.interval_tree.IntervalTree
import com.github.nava2.aff4.io.Seekable
import com.github.nava2.aff4.streams.image_stream.Aff4ImageStreamSink
import okio.Buffer
import okio.Timeout
import org.eclipse.rdf4j.model.IRI

internal class SeekableMapDataStreamChunkSink(
  private val dataStreamSink: Aff4ImageStreamSink,
  private val timeout: Timeout,
) : MapDataChunkSink, Seekable {

  /** Buffer used to write to the [dataStreamSink] */
  private val dataStreamBuffer = Buffer()

  val entryTree: IntervalTree<MapStreamEntry> = IntervalTree()

  private val _idxEntries = mutableMapOf<IRI, Int>()

  val idxEntries: Map<IRI, Int> get() = _idxEntries

  override var position: Long = 0L

  @Volatile
  private var closed = false

  override fun write(chunk: MapDataChunk) {
    check(!closed) { "closed" }

    timeout.throwIfReached()

    val mapStreamEntry = when (chunk) {
      is MapDataChunk.Symbolic -> chunk.asMapStreamEntry(mappedOffset = position)
      is MapDataChunk.Data -> writeChunkToImageStream(chunk)
    }

    writeEntryToTree(mapStreamEntry)

    position += mapStreamEntry.length
  }

  override fun flush() {
    check(!closed) { "closed" }
    dataStreamSink.flush()
  }

  override fun timeout(): Timeout = timeout

  override fun close() {
    if (closed) return
    synchronized(this) {
      if (closed) return
      closed = true
    }

    dataStreamSink.close()
  }

  private fun writeEntryToTree(mapStreamEntry: MapStreamEntry) {
    _idxEntries.putIfAbsent(mapStreamEntry.targetIRI, _idxEntries.size)

    // TODO support this
    check(!entryTree.overlaps(mapStreamEntry.mappedOffset, mapStreamEntry.length)) {
      "Attempted to rewrite an existing map entry: $mapStreamEntry"
    }

    entryTree.insert(mapStreamEntry)
  }

  private fun writeChunkToImageStream(chunk: MapDataChunk.Data): MapStreamEntry {
    val dataStreamOffset = dataStreamSink.dataPosition

    dataStreamBuffer.write(chunk.data)
    dataStreamSink.write(dataStreamBuffer, dataStreamBuffer.size)

    return MapStreamEntry(
      mappedOffset = position,
      length = chunk.length,
      targetOffset = dataStreamOffset,
      targetIRI = dataStreamSink.imageStream.arn,
    )
  }
}

private fun MapDataChunk.Symbolic.asMapStreamEntry(mappedOffset: Long): MapStreamEntry = MapStreamEntry(
  mappedOffset = mappedOffset,
  length = length,
  targetOffset = mappedOffset,
  targetIRI = symbolArn,
)
