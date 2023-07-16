package net.navatwo.kaff4.api

import com.google.inject.Binder
import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.guice.KSetMultibinderHelper
import net.navatwo.kaff4.model.rdf.Aff4RdfModel
import net.navatwo.kaff4.model.rdf.CompressionMethod
import net.navatwo.kaff4.rdf.RdfRepositoryConfiguration
import net.navatwo.kaff4.rdf.RdfValueConverter
import kotlin.reflect.KClass

/**
 * Defines a KAff4 plugin that adds behaviour into the KAFF4 system.
 *
 * This is a wrapper around a Guice [com.google.inject.Module].
 */
abstract class KAff4FeatureModule protected constructor() : KAff4AbstractModule() {

  final override fun configure() {
    binder().requireAtInjectOnConstructors()

    bindRdfValueConverters {}
    bindCompressionMethods {}
    bindAff4Models {}

    configurePlugin()
  }

  protected abstract fun configurePlugin()

  override fun binder(): Binder = super.binder().skipSources(KAff4FeatureModule::class.java)

  protected fun bindRdfRepositoryConfiguration(): KotlinAnnotatedBindingBuilder<in RdfRepositoryConfiguration> = bind()

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
}
