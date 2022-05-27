package com.github.nava2.aff4.model

import com.github.nava2.aff4.io.SourceProvider
import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import okio.Source

interface Aff4StreamSourceProvider : SourceProvider<Source> {

  val size: Long

  interface Loader<in C : Aff4RdfModel, out S : Aff4StreamSourceProvider> {
    fun load(config: C): S
  }
}
