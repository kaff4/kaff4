package net.navatwo.kaff4.streams.image_stream

import net.navatwo.kaff4.model.Aff4StreamSourceProvider
import net.navatwo.kaff4.model.VerifiableStreamProvider
import net.navatwo.kaff4.model.rdf.ImageStream
import java.io.Closeable

interface Aff4ImageStreamSourceProvider : VerifiableStreamProvider, Aff4StreamSourceProvider, Closeable {
  val imageStream: ImageStream

  interface Loader : Aff4StreamSourceProvider.Loader<ImageStream, Aff4ImageStreamSourceProvider>
}
