package com.github.nava2.aff4

import com.github.nava2.aff4.meta.rdf.RdfRepositoryModule
import com.github.nava2.aff4.meta.rdf.io.RdfModelParserModule
import com.github.nava2.aff4.meta.rdf.model.Aff4RdfModelModule
import com.github.nava2.guice.KAbstractModule
import com.google.inject.BindingAnnotation
import okio.FileSystem

@Target(
  AnnotationTarget.PROPERTY,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FIELD,
)
@BindingAnnotation
annotation class ForResources

@Target(
  AnnotationTarget.PROPERTY,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FIELD,
)
@BindingAnnotation
annotation class ForImages

object Aff4CoreModule : KAbstractModule() {
  override fun configure() {
    bind<FileSystem>()
      .annotatedWith(ForResources::class.java)
      .toInstance(FileSystem.RESOURCES)

    install(RdfRepositoryModule)
    install(RdfModelParserModule)
    install(Aff4RdfModelModule)
  }
}
