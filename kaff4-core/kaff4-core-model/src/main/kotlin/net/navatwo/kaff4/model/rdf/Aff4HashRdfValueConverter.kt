package net.navatwo.kaff4.model.rdf

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.navatwo.guice.typeLiteral
import net.navatwo.kaff4.rdf.io.ConcreteRdfValueConverter
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value

@Singleton
internal class Aff4HashRdfValueConverter @Inject constructor() : ConcreteRdfValueConverter<Hash>(typeLiteral<Hash>()) {
  override fun parse(value: Value): Hash? {
    return (value as? Literal)?.let { Hash.fromLiteral(it) }
  }

  override fun serialize(value: Hash): Value {
    return valueFactory.createLiteral(value.value.hex(), valueFactory.createAff4Iri(value.hashType.name))
  }
}
