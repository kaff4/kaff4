package com.github.nava2.aff4.streams.map_stream

import okio.Timeout
import org.eclipse.rdf4j.model.IRI
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
    val symbolArn: IRI,
    override val length: Long,
  ) : MapDataChunk {

    constructor(symbolArn: IRI, length: Int) : this(symbolArn, length.toLong())
  }

  data class Data(
    val data: ByteBuffer,
  ) : MapDataChunk {
    override val length: Long = data.limit().toLong()
  }
}
