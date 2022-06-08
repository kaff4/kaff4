package com.github.nava2.aff4

import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.guice.KAbstractModule
import com.google.inject.Provides
import okio.FileSystem
import okio.Path.Companion.toPath
import javax.inject.Singleton

object TestImagesModule : KAbstractModule() {
  override fun configure() {
    bind<FileSystem>()
      .annotatedWith(ForResources::class.java)
      .toInstance(FileSystem.RESOURCES)
  }

  @Provides
  @Singleton
  @ForImages
  fun providesFileSystemForImages(@ForResources resourcesFileSystem: FileSystem): FileSystem {
    return resourcesFileSystem.relativeTo("images".toPath())
  }
}
