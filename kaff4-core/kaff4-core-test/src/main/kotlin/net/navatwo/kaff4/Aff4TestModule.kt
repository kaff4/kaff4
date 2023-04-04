package net.navatwo.kaff4

import com.google.inject.Provides
import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.kaff4.io.relativeTo
import net.navatwo.kaff4.model.rdf.Aff4RdfModelPlugin
import net.navatwo.kaff4.rdf.MemoryRdfRepositoryPlugin
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
