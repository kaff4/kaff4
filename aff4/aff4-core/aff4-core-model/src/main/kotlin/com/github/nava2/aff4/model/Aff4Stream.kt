package com.github.nava2.aff4.model

import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import okio.Source

interface Aff4Stream : AutoCloseable {
  fun source(position: Long): Source

  interface Loader<in C : Aff4RdfModel, out S : Aff4Stream> {
    fun load(config: C): S
  }
}
