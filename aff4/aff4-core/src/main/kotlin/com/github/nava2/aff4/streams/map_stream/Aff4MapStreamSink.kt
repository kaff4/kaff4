package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.io.Seekable
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.streams.image_stream.Aff4ImageStreamSink
import com.github.nava2.aff4.streams.symbolics.Symbolics
import okio.Buffer
import okio.FileSystem
import okio.Sink
import okio.Timeout
import okio.buffer
import okio.use
import java.util.Random
import javax.inject.Named

private const val CHUNKED_ENTRY_SHUFFLED_SIZE = 128

internal class Aff4MapStreamSink(
  private val random: Random,
  symbolics: Symbolics,
  @Named("ImageOutput") private val outputFileSystem: FileSystem,
  private val dataStreamSink: Aff4ImageStreamSink,
  mapStream: MapStream,
  private val timeout: Timeout,
) : Sink, Seekable {
  var mapStream: MapStream = mapStream
    get() {
      check(closed) { "do not use until closing stream" }
      return field
    }
    private set

  var size: Long = 0L
    private set

  private var _position: Long = 0L

  override var position: Long
    get() = _position
    set(value) {
      check(!closed) { "closed" }

      flush()
      _position = value
      dataChunkSink.position = value
    }

  private val dataChunkSink = SeekableMapDataStreamChunkSink(dataStreamSink, timeout)

  private val dataChunker = MapStreamDataChunkerBufferedSink(
    symbolics = symbolics,
    timeout = timeout,
    chunkSize = dataStreamSink.imageStream.chunkSize,
    mapDataChunkSink = dataChunkSink,
  )

  @Volatile
  private var closed: Boolean = false

  override fun write(source: Buffer, byteCount: Long) {
    check(!closed) { "closed" }

    dataChunker.write(source, byteCount)

    _position += byteCount
    size = maxOf(size, position)
  }

  override fun close() {
    if (closed) return
    synchronized(this) {
      if (closed) return
      closed = true
    }

    dataChunker.close()
    dataChunkSink.close()
    dataStreamSink.close()

    writeMapTargets()
    writeMapMap()

    mapStream = mapStream.copy(size = size)
  }

  private fun writeMapMap() {
    outputFileSystem.sink(mapStream.mapPath(mapStream.stored!!), mustCreate = true).buffer().use { mapEntrySink ->
      // We intentionally shuffle the chunks to write them. This is an attempt to avoid the worst case interval inserts
      // for reader implementations - Be Kind!
      val shuffledEntryChunks = dataChunkSink.entryTree.compressedSequence()
        .chunked(CHUNKED_ENTRY_SHUFFLED_SIZE) {
          it.shuffled(random)
        }

      for (mapStreamEntries in shuffledEntryChunks) {
        for (entry in mapStreamEntries) {
          entry.writeToSink(dataChunkSink.idxEntries, mapEntrySink)
        }
      }
    }
  }

  private fun writeMapTargets() {
    outputFileSystem.sink(mapStream.idxPath(mapStream.stored!!), mustCreate = true).buffer().use { mapIdxSink ->
      val targetIriSortedByIndex = dataChunkSink.idxEntries.entries.asSequence().sortedBy { it.value }.map { it.key }

      for (target in targetIriSortedByIndex) {
        mapIdxSink.writeUtf8(target.stringValue())
        mapIdxSink.writeUtf8("\n")
      }
    }
  }

  override fun flush() {
    check(!closed) { "closed" }

    dataChunker.flush()
  }

  override fun timeout(): Timeout = timeout
}
