package com.github.nava2.aff4.streams

import com.github.nava2.aff4.io.SizedSink
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.Aff4RdfModel

interface Aff4Sink : SizedSink {
  val arn: Aff4Arn

  val model: Aff4RdfModel
}
