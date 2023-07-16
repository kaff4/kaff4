package net.navatwo.kaff4.streams.map_stream

import net.navatwo.kaff4.model.Aff4StreamSourceProvider
import net.navatwo.kaff4.model.VerifiableStreamProvider
import net.navatwo.kaff4.model.rdf.MapStream

internal interface Aff4MapStreamSourceProvider : Aff4StreamSourceProvider, VerifiableStreamProvider {
  val mapStream: MapStream

  interface Loader : Aff4StreamSourceProvider.Loader<MapStream, Aff4MapStreamSourceProvider>
}
