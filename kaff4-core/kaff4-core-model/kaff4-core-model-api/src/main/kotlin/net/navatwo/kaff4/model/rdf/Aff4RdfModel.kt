package net.navatwo.kaff4.model.rdf

import net.navatwo.kaff4.model.rdf.annotations.RdfSubject

interface Aff4RdfModel {
  @RdfSubject
  val arn: Aff4Arn
}
