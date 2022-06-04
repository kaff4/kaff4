package com.github.nava2.aff4.rdf.io.literals

import com.github.nava2.aff4.rdf.io.ConcreteRdfValueConverter
import com.github.nava2.guice.typeLiteral
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Value
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ResourceRdfConverter @Inject constructor() :
  ConcreteRdfValueConverter<Resource>(typeLiteral<Resource>()) {
  override fun parse(value: Value): Resource? = value as? Resource
  override fun serialize(value: Resource): Value = value
}
