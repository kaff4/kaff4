package com.github.nava2.aff4

import com.github.nava2.aff4.container.Aff4ContainerOpenerModule
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.Aff4ContainerOpener
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryModule
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.test.GuiceExtension
import com.google.inject.Module
import com.google.inject.Provides
import okio.FileSystem
import okio.Path.Companion.toPath
import javax.inject.Singleton

class Aff4ImageTestModule(val imageName: String, vararg featureModules: Module) : KAbstractModule() {
  private val featureModules = featureModules.toSet()

  override fun configure() {
    install(TestImagesModule)

    install(Aff4ContainerOpenerModule)
    install(Aff4CoreModule)
    install(Aff4BaseStreamModule)
    install(MemoryRdfRepositoryModule)
    install(TestRandomsModule)
  }

  @Provides
  @UnderTest
  @Singleton
  internal fun providesAff4ContainerUnderTest(
    @UnderTest aff4ContainerOpener: Aff4ContainerOpener,
    cleanupActions: GuiceExtension.CleanupActions,
    @ForImages imagesFileSystem: FileSystem,
  ): Aff4Container {
    check(imagesFileSystem.exists(imageName.toPath())) {
      "Image path does not exist: ${imagesFileSystem.canonicalize(imageName.toPath())}"
    }

    return aff4ContainerOpener.manualOpen(imagesFileSystem, imageName.toPath())
      .apply(cleanupActions::register)
      .aff4Container
  }

  @Provides
  @Singleton
  @UnderTest
  internal fun providesAff4ModelUnderTest(@UnderTest aff4Container: Aff4Container) = aff4Container.aff4Model

  @Provides
  @Singleton
  @UnderTest
  internal fun providesAff4StreamOpenerUnderTest(@UnderTest aff4Container: Aff4Container) = aff4Container.streamOpener
}
