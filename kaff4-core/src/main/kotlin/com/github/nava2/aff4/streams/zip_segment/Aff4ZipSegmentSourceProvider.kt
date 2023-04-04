package com.github.nava2.aff4.streams.zip_segment

import com.github.nava2.aff4.model.Aff4StreamSourceProvider
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.rdf.ZipSegment

interface Aff4ZipSegmentSourceProvider : Aff4StreamSourceProvider, VerifiableStreamProvider {
  val zipSegment: ZipSegment

  interface Loader : Aff4StreamSourceProvider.Loader<ZipSegment, Aff4ZipSegmentSourceProvider>
}
