package net.navatwo.kaff4.streams.zip_segment

import net.navatwo.kaff4.model.Aff4StreamSourceProvider
import net.navatwo.kaff4.model.VerifiableStreamProvider
import net.navatwo.kaff4.model.rdf.ZipSegment

interface Aff4ZipSegmentSourceProvider : Aff4StreamSourceProvider, VerifiableStreamProvider {
  val zipSegment: ZipSegment

  interface Loader : Aff4StreamSourceProvider.Loader<ZipSegment, Aff4ZipSegmentSourceProvider>
}
