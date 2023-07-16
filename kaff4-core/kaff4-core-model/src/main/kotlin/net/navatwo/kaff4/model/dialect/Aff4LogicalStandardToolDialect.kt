package net.navatwo.kaff4.model.dialect

import com.google.inject.Provides
import jakarta.inject.Singleton
import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.kaff4.api.InternalApi
import net.navatwo.kaff4.model.Aff4Container
import net.navatwo.kaff4.model.rdf.Aff4RdfModel
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

  @InternalApi
  @Target(CLASS)
  @Retention(RUNTIME)
  annotation class RdfStandardType(val rdfType: String)

  @InternalApi
  object Module : KAff4AbstractModule() {
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
