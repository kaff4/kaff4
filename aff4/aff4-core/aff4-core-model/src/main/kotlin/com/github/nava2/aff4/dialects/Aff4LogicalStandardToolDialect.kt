package com.github.nava2.aff4.dialects

import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.dialect.Aff4ToolDialectModule
import com.github.nava2.aff4.model.dialect.DialectTypeResolver
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.guice.KAbstractModule
import com.google.inject.Provides
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

@Singleton
class Aff4LogicalStandardToolDialect internal constructor(
  override val typeResolver: DialectTypeResolver,
) : ToolDialect {

  override fun isApplicable(toolMetadata: Aff4Container.ToolMetadata): Boolean {
    return true
  }

  @Target(CLASS)
  @Retention(RUNTIME)
  annotation class RdfStandardType(val rdfType: String)

  object Module : KAbstractModule() {
    override fun configure() {
      install(Aff4ToolDialectModule)
    }

    @Singleton
    @Provides
    internal fun providesAff4LogicalStandardDialect(
      modelKlasses: Set<KClass<out Aff4RdfModel>>,
    ): Aff4LogicalStandardToolDialect {
      val typeResolver = DialectTypeResolver.Builder.forAnnotation(RdfStandardType::class) {
        rdfType to setOf(rdfType)
      }
        .apply {
          for (modelKlass in modelKlasses) {
            register(modelKlass)
          }
        }
        .build()

      return Aff4LogicalStandardToolDialect(typeResolver)
    }
  }
}
