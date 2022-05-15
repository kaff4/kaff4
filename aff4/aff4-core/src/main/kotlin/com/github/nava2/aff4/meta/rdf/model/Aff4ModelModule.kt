package com.github.nava2.aff4.meta.rdf.model

import com.github.nava2.aff4.meta.rdf.io.ConcreteRdfValueConverter
import com.github.nava2.aff4.meta.rdf.io.RdfValueConverter
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.typeLiteral
import okio.Path
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.reflect.KClass

object Aff4ModelModule : KAbstractModule() {
  override fun configure() {
    bindSet<KClass<out Aff4RdfModel>> {
      for (subclass in Aff4RdfModel::class.sealedSubclasses) {
        toInstance(subclass)
      }
    }

    bindSet<CompressionMethod> {}

    bindSet<RdfValueConverter<*>> {
      toInstance(Aff4ImagePathRdfValueConverter)
      toInstance(Aff4HashRdfValueConverter)
      to<Aff4CompressionMethodValueConverter>()
    }
  }
}

internal object Aff4ImagePathRdfValueConverter : ConcreteRdfValueConverter<Path>(typeLiteral<Path>()) {
  override fun convert(value: Value): Path? {
    val path = (value as? Literal)?.label ?: return null
    return path.toPath()
  }
}

internal object Aff4HashRdfValueConverter : ConcreteRdfValueConverter<Hash>(typeLiteral<Hash>()) {
  override fun convert(value: Value): Hash? {
    return (value as? Literal)?.let { Hash.fromLiteral(it) }
  }
}

@Singleton
internal class Aff4CompressionMethodValueConverter @Inject constructor(
  private val compressionMethodProviders: Provider<Set<CompressionMethod>>,
) : ConcreteRdfValueConverter<CompressionMethod>(typeLiteral<CompressionMethod>()) {
  override fun convert(value: Value): CompressionMethod? {
    val method = value as? IRI ?: return null
    return compressionMethodProviders.get().firstOrNull { it.method == method }
  }
}
