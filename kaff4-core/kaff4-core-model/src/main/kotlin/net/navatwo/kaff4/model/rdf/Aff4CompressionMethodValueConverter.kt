package net.navatwo.kaff4.model.rdf

import net.navatwo.guice.typeLiteral
import net.navatwo.kaff4.rdf.io.ConcreteRdfValueConverter
import org.eclipse.rdf4j.model.Value
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
internal class Aff4CompressionMethodValueConverter @Inject constructor(
  private val compressionMethodProviders: Provider<Set<CompressionMethod>>,
) : ConcreteRdfValueConverter<CompressionMethod>(typeLiteral<CompressionMethod>()) {
  override fun parse(value: Value): CompressionMethod? {
    val method = value as? Aff4Arn ?: return null
    return compressionMethodProviders.get().firstOrNull { it.method == method.toString() }
  }

  override fun serialize(value: CompressionMethod): Value? = value.method?.let { valueFactory.createIRI(it) }
}
