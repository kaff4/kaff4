package com.github.nava2.aff4

import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.model.rdf.Aff4RdfModelPlugin
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryPlugin
import com.github.nava2.guice.KAff4AbstractModule
import com.google.inject.Provides
import okio.FileSystem
import okio.Path.Companion.toPath
import javax.inject.Singleton

object Aff4TestModule : KAff4AbstractModule() {
  override fun configure() {
    bind<FileSystem>()
      .annotatedWith(ForResources::class.java)
      .toInstance(FileSystem.RESOURCES)

    install(TestRandomsModule)
    install(MemoryRdfRepositoryPlugin)
    install(Aff4RdfModelPlugin)
  }

  @Provides
  @Singleton
  @ForImages
  internal fun providesFileSystemForImages(@ForResources resourcesFileSystem: FileSystem): FileSystem {
    return resourcesFileSystem.relativeTo("images".toPath())
  }
}
