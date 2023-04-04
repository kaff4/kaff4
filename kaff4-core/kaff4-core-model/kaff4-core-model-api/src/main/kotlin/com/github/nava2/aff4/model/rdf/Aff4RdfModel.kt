package com.github.nava2.aff4.model.rdf

import com.github.nava2.aff4.model.rdf.annotations.RdfSubject

interface Aff4RdfModel {
  @RdfSubject
  val arn: Aff4Arn
}
