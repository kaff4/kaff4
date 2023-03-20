package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.model.Aff4StreamSourceProvider
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.rdf.ImageStream
import java.io.Closeable

interface Aff4ImageStreamSourceProvider : VerifiableStreamProvider, Aff4StreamSourceProvider, Closeable {
  val imageStream: ImageStream

  interface Loader : Aff4StreamSourceProvider.Loader<ImageStream, Aff4ImageStreamSourceProvider>
}
