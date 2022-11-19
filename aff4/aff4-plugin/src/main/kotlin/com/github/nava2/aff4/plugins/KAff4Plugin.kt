package com.github.nava2.aff4.plugins

import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.CompressionMethod
import com.github.nava2.aff4.rdf.RdfRepositoryConfiguration
import com.github.nava2.aff4.rdf.RdfValueConverter
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.KSetMultibinderHelper
import com.google.inject.binder.AnnotatedBindingBuilder
import javax.inject.Qualifier
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.reflect.KClass

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

    bindRdfValueConverters {}
    bindCompressionMethods {}
    bindAff4Models {}

    configurePlugin()
  }

  protected abstract fun configurePlugin()

  protected fun bindRdfRepositoryConfiguration(): AnnotatedBindingBuilder<RdfRepositoryConfiguration> = bind()

  protected inline fun bindRdfValueConverters(
    crossinline block: KSetMultibinderHelper<RdfValueConverter<*>>.() -> Unit,
  ) {
    bindSet { block() }
  }

  protected inline fun bindCompressionMethods(
    crossinline block: KSetMultibinderHelper<CompressionMethod>.() -> Unit,
  ) {
    bindSet { block() }
  }

  protected inline fun bindAff4Models(
    crossinline block: KSetMultibinderHelper<KClass<out Aff4RdfModel>>.() -> Unit,
  ) {
    bindSet { block() }
  }

  data class PluginIdentifier(val name: String)

  @Qualifier
  @Target(PROPERTY, VALUE_PARAMETER)
  annotation class Identifiers
}
