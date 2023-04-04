package net.navatwo.kaff4.rdf.io.literals

import net.navatwo.guice.typeLiteral
import net.navatwo.kaff4.rdf.io.ConcreteRdfValueConverter
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Value
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class IriRdfConverter @Inject constructor() : ConcreteRdfValueConverter<IRI>(typeLiteral<IRI>()) {
  override fun parse(value: Value): IRI? = value as? IRI
  override fun serialize(value: IRI): Value = value
}
