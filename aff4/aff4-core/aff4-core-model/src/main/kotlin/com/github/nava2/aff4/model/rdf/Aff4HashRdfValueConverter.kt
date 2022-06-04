package com.github.nava2.aff4.model.rdf

import com.github.nava2.aff4.rdf.io.ConcreteRdfValueConverter
import com.github.nava2.guice.typeLiteral
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class Aff4HashRdfValueConverter @Inject constructor() : ConcreteRdfValueConverter<Hash>(typeLiteral<Hash>()) {
  override fun parse(value: Value): Hash? {
    return (value as? Literal)?.let { Hash.fromLiteral(it) }
  }

  override fun serialize(value: Hash): Value {
    return valueFactory.createLiteral(value.value.hex(), valueFactory.createAff4Iri(value.hashType.name))
  }
}
