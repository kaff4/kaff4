package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.model.Aff4StreamSourceProvider
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.rdf.MapStream

interface Aff4MapStreamSourceProvider : Aff4StreamSourceProvider, VerifiableStreamProvider {
  val mapStream: MapStream

  interface Loader : Aff4StreamSourceProvider.Loader<MapStream, Aff4MapStreamSourceProvider>
}
