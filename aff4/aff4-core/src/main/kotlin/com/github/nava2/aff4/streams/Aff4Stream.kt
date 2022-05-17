package com.github.nava2.aff4.streams

import com.github.nava2.aff4.meta.rdf.model.Aff4RdfModel
import okio.Source

interface Aff4Stream : AutoCloseable {
  fun source(position: Long): Source

  interface Loader<C : Aff4RdfModel, out S : Aff4Stream> {
    fun load(config: C): S
  }
}
