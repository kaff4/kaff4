package com.github.nava2.aff4.meta.rdf.parser

import com.github.nava2.aff4.ForImages
import com.github.nava2.aff4.io.RelativeFileSystem
import com.github.nava2.guice.KAbstractModule
import com.google.inject.BindingAnnotation
import com.google.inject.Provides
import okio.FileSystem
import okio.Path
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
annotation class ForImageRoot

internal class ScopedParserModule(
  private val imagePath: Path,
) : KAbstractModule() {
  override fun configure() = Unit

  @ForImageRoot
  @Singleton
  @Provides
  fun providesImageRootFileSystem(@ForImages imagesFileSystem: FileSystem): FileSystem {
    return RelativeFileSystem(imagesFileSystem, imagePath)
  }
}
