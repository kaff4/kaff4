package net.navatwo.kaff4.streams.map_stream

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import net.navatwo.kaff4.io.SeekableSink
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.streams.Aff4Sink
import net.navatwo.kaff4.streams.image_stream.Aff4ImageStreamSink
import net.navatwo.kaff4.streams.symbolics.Symbolics
import okio.Buffer
import okio.FileSystem
import okio.Timeout
import okio.buffer
import okio.use
import java.util.Random

private const val CHUNKED_ENTRY_SHUFFLED_SIZE = 128

internal class Aff4MapStreamSink @AssistedInject constructor(
  private val random: Random,
  symbolics: Symbolics,
  @Assisted private val outputFileSystem: FileSystem,
  @Assisted private val dataStreamSink: Aff4ImageStreamSink,
  @Assisted mapStream: MapStream,
  @Assisted private val timeout: Timeout,
) : SeekableSink, Aff4Sink {
  override val arn: Aff4Arn = mapStream.arn

  var mapStream: MapStream = mapStream
    get() {
      check(closed) { "do not use until closing stream" }
      return field
    }
    private set

  override val model: MapStream by ::mapStream

  private var _position: Long = 0L

  override var position: Long
    get() = _position
    set(value) {
      check(!closed) { "closed" }

      flush()
      _position = value
      dataChunkSink.position = value
    }

  override var size: Long = 0L
    private set

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
    outputFileSystem.sink(mapStream.mapPath, mustCreate = true).buffer().use { mapEntrySink ->
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
    outputFileSystem.sink(mapStream.idxPath, mustCreate = true).buffer().use { mapIdxSink ->
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
