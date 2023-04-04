package net.navatwo.kaff4.streams.map_stream

import com.github.nava2.interval_tree.IntervalTree
import net.navatwo.kaff4.io.Seekable
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.streams.Aff4Sink
import okio.Buffer
import okio.Timeout

internal class SeekableMapDataStreamChunkSink(
  private val dataStreamSink: Aff4Sink,
  private val timeout: Timeout,
) : MapDataChunkSink, Seekable {

  /** Buffer used to write to the [dataStreamSink] */
  private val dataStreamBuffer = Buffer()

  val entryTree: IntervalTree<MapStreamEntry> = IntervalTree()

  private val _idxEntries = mutableMapOf<Aff4Arn, Int>()

  val idxEntries: Map<Aff4Arn, Int> get() = _idxEntries

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
    _idxEntries.putIfAbsent(mapStreamEntry.targetArn, _idxEntries.size)

    // TODO support this
    check(!entryTree.overlaps(mapStreamEntry.mappedOffset, mapStreamEntry.length)) {
      "Attempted to rewrite an existing map entry: $mapStreamEntry"
    }

    entryTree.insert(mapStreamEntry)
  }

  private fun writeChunkToImageStream(chunk: MapDataChunk.Data): MapStreamEntry {
    val dataStreamOffset = dataStreamSink.size

    dataStreamBuffer.write(chunk.data)
    dataStreamSink.write(dataStreamBuffer, dataStreamBuffer.size)

    return MapStreamEntry(
      mappedOffset = position,
      length = chunk.length,
      targetOffset = dataStreamOffset,
      targetArn = dataStreamSink.arn,
    )
  }
}

private fun MapDataChunk.Symbolic.asMapStreamEntry(mappedOffset: Long): MapStreamEntry = MapStreamEntry(
  mappedOffset = mappedOffset,
  length = length,
  targetOffset = mappedOffset,
  targetArn = symbolArn,
)
