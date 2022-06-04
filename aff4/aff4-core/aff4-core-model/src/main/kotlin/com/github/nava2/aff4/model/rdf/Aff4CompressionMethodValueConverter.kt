package com.github.nava2.aff4.model.rdf

import com.github.nava2.aff4.rdf.io.ConcreteRdfValueConverter
import com.github.nava2.guice.typeLiteral
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Value
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
internal class Aff4CompressionMethodValueConverter @Inject constructor(
  private val compressionMethodProviders: Provider<Set<CompressionMethod>>,
) : ConcreteRdfValueConverter<CompressionMethod>(typeLiteral<CompressionMethod>()) {
  override fun parse(value: Value): CompressionMethod? {
    val method = value as? IRI ?: return null
    return compressionMethodProviders.get().firstOrNull { it.method == method }
  }

  override fun serialize(value: CompressionMethod): Value? = value.method
}
