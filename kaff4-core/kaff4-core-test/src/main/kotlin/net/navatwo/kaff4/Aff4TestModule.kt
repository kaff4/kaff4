package net.navatwo.kaff4

import com.google.inject.Provides
import jakarta.inject.Singleton
import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.kaff4.io.relativeTo
import net.navatwo.kaff4.model.rdf.Aff4RdfModelFeatureModule
import net.navatwo.kaff4.rdf.MemoryRdfRepositoryFeatureModule
import okio.FileSystem
import okio.Path.Companion.toPath

object Aff4TestModule : KAff4AbstractModule() {
  override fun configure() {
    bind<FileSystem>()
      .annotatedWith(ForResources::class.java)
      .toInstance(FileSystem.RESOURCES)

    install(TestRandomsModule)
    install(MemoryRdfRepositoryFeatureModule)
    install(Aff4RdfModelFeatureModule)
  }

  @Provides
  @Singleton
  @ForImages
  internal fun providesFileSystemForImages(@ForResources resourcesFileSystem: FileSystem): FileSystem {
    return resourcesFileSystem.relativeTo("images".toPath())
  }
}
