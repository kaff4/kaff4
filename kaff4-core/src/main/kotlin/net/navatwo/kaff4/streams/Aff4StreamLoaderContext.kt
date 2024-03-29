package net.navatwo.kaff4.streams

import com.google.inject.TypeLiteral
import jakarta.inject.Provider
import net.navatwo.kaff4.model.Aff4StreamSourceProvider
import net.navatwo.kaff4.model.rdf.Aff4RdfModel

internal class Aff4StreamLoaderContext(
  val configTypeLiteral: TypeLiteral<out Aff4RdfModel>,
  private val loaderProvider: Provider<out Aff4StreamSourceProvider.Loader<*, Aff4StreamSourceProvider>>,
) {
  fun get(): Aff4StreamSourceProvider.Loader<Aff4RdfModel, Aff4StreamSourceProvider> {
    @Suppress("UNCHECKED_CAST")
    return loaderProvider.get() as Aff4StreamSourceProvider.Loader<Aff4RdfModel, Aff4StreamSourceProvider>
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Aff4StreamLoaderContext) return false

    return configTypeLiteral == other.configTypeLiteral
  }

  override fun hashCode(): Int {
    return configTypeLiteral.hashCode()
  }
}
