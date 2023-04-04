package net.navatwo.kaff4.streams

import net.navatwo.kaff4.io.SizedSink
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.Aff4RdfModel

interface Aff4Sink : SizedSink {
  val arn: Aff4Arn

  val model: Aff4RdfModel
}
