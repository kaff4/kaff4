package com.github.nava2.aff4.meta.parser

import com.github.nava2.aff4.ForImages
import com.github.nava2.aff4.io.RelativeFileSystem
import com.github.nava2.aff4.meta.Hash
import com.github.nava2.aff4.meta.rdf.io.ConcreteRdfValueConverter
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.typeLiteral
import com.google.inject.BindingAnnotation
import com.google.inject.Provides
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import javax.inject.Singleton

/**
 * Defines a [FileSystem] that is rooted at the folder in which an image lives.
 */
@Target(
  AnnotationTarget.PROPERTY,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FIELD,
)
@BindingAnnotation
annotation class ForImageFolder

internal class ScopedParserModule(
  private val imagePath: Path,
) : KAbstractModule() {
  override fun configure() = Unit

  @ForImageFolder
  @Singleton
  @Provides
  fun providesImageRootFileSystem(@ForImages imagesFileSystem: FileSystem): FileSystem {
    return RelativeFileSystem(imagesFileSystem, imagePath)
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
