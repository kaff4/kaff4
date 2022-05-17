package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.meta.rdf.model.MapStream
import com.github.nava2.aff4.streams.Aff4Stream
import okio.Source

class Aff4MapStream : Aff4Stream {
  override fun source(position: Long): Source {
    TODO("Not yet implemented")
  }

  override fun close() {
    TODO("Not yet implemented")
  }

  interface Loader : Aff4Stream.Loader<MapStream, Aff4MapStream>
}
