package com.github.nava2.aff4.rdf.io.literals

import com.github.nava2.aff4.rdf.io.ConcreteRdfValueConverter
import com.github.nava2.guice.typeLiteral
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Value
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class IriRdfConverter @Inject constructor() : ConcreteRdfValueConverter<IRI>(typeLiteral<IRI>()) {
  override fun parse(value: Value): IRI? = value as? IRI
  override fun serialize(value: IRI): Value = value
}
