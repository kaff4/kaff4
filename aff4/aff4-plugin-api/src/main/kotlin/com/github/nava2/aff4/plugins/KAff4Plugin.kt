package com.github.nava2.aff4.plugins

import com.github.nava2.aff4.model.rdf.CompressionMethod
import com.github.nava2.aff4.rdf.RdfRepositoryConfiguration
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.KSetMultibinderHelper
import javax.inject.Qualifier
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * Defines a KAff4 plugin that adds behaviour into the KAFF4 system.
 *
 * This is a wrapper around a Guice [com.google.inject.Module].
 */
abstract class KAff4Plugin protected constructor(
  pluginIdentifier: String,
) : KAbstractModule() {

  val pluginIdentifier = PluginIdentifier(name = pluginIdentifier)

  final override fun configure() {
    binder().requireAtInjectOnConstructors()

    bindSet<PluginIdentifier>(Identifiers::class) {
      toInstance(pluginIdentifier)
    }

    configurePlugin()
  }

  protected abstract fun configurePlugin()

  protected inline fun bindCompressionMethods(block: KSetMultibinderHelper<CompressionMethod>.() -> Unit) {
    bindSet { block() }
  }

  protected fun bindRdfRepositoryConfiguration() = bind<RdfRepositoryConfiguration>()

  data class PluginIdentifier(val name: String)

  @Qualifier
  @Target(PROPERTY, VALUE_PARAMETER)
  annotation class Identifiers
}
