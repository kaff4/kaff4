package net.navatwo.kaff4.model

import net.navatwo.kaff4.io.SourceProvider
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.Aff4RdfModel
import okio.Source

interface Aff4StreamSourceProvider : SourceProvider<Source> {
  val arn: Aff4Arn

  val size: Long

  interface Loader<in C : Aff4RdfModel, out S : Aff4StreamSourceProvider> {
    fun load(config: C): S
  }
}
