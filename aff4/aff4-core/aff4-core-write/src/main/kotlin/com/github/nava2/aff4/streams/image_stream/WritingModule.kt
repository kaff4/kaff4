package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.guice.KAbstractModule
import com.google.inject.Provides
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import java.nio.file.Files
import javax.inject.Named
import javax.inject.Singleton

internal class WritingModule(val outputPath: Path) : KAbstractModule() {
  override fun configure() = Unit

  @Provides
  @Singleton
  @Named("ImageOutput")
  fun providesImageOutputFileSystem(
    sha256FileSystemFactory: Sha256FileSystemFactory,
  ): FileSystem {
    val outputFileName = outputPath.name.replace('.', '_')
    val imageTempDirectory = Files.createTempDirectory("aff4k_${outputFileName}_")
    return sha256FileSystemFactory.create(imageTempDirectory.toOkioPath(normalize = true))
  }
}
