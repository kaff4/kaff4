package net.navatwo.kaff4.streams.map_stream

import net.navatwo.kaff4.model.rdf.Aff4Arn
import okio.Timeout
import java.io.Closeable
import java.io.Flushable
import java.nio.ByteBuffer

interface MapDataChunkSink : Closeable, Flushable {
  fun write(chunk: MapDataChunk)

  fun timeout(): Timeout
}

sealed interface MapDataChunk {
  val length: Long

  data class Symbolic(
    val symbolArn: Aff4Arn,
    override val length: Long,
  ) : MapDataChunk {

    constructor(symbolArn: Aff4Arn, length: Int) : this(symbolArn, length.toLong())
  }

  data class Data(
    val data: ByteBuffer,
  ) : MapDataChunk {
    override val length: Long = data.limit().toLong()
  }
}
