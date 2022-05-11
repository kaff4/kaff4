package com.github.nava2.aff4.meta.parser

import com.github.nava2.aff4.ForImages
import com.github.nava2.aff4.io.RelativeFileSystem
import com.github.nava2.aff4.meta.Aff4Model
import com.github.nava2.aff4.meta.BlockHashes
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
annotation class ForImageFolder

internal class ScopedParserModule(
  private val imagePath: Path,
) : KAbstractModule() {
  override fun configure() {
    bindSet<Aff4Model.Parser<*>> {
      to<BlockHashes.Parser>()
//      to<ZipVolume.Parser>()
    }
  }

  @ForImageFolder
  @Singleton
  @Provides
  fun providesImageRootFileSystem(@ForImages imagesFileSystem: FileSystem): FileSystem {
    return RelativeFileSystem(imagesFileSystem, imagePath)
  }
}
