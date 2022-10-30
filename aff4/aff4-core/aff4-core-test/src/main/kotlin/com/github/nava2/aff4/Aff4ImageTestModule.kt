package com.github.nava2.aff4

import com.github.nava2.aff4.container.Aff4ContainerOpenerBuilder
import com.github.nava2.aff4.container.RealAff4ContainerOpener
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.Aff4ContainerOpener
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryModule
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.getInstance
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
  }

  @Provides
  @UnderTest
  @Singleton
  internal fun providesAff4ContainerOpenerUnderTest(
    @ForImages imagesFileSystem: FileSystem,
    aff4ContainerOpenerBuilder: Aff4ContainerOpenerBuilder,
  ): Aff4ContainerOpener {
    check(imagesFileSystem.exists(imageName.toPath())) {
      "Image path does not exist: ${imagesFileSystem.canonicalize(imageName.toPath())}"
    }

    val aff4ContainerOpener = aff4ContainerOpenerBuilder
      .withFeatureModules(featureModules)
      .build() as RealAff4ContainerOpener

    val testInjector = aff4ContainerOpener.setupContainerInjector(
      fileSystem = imagesFileSystem,
      path = imageName.toPath(),
      extraModules = setOf(MemoryRdfRepositoryModule, TestRandomsModule, TestImagesModule),
    )

    return testInjector.getInstance()
  }

  @Provides
  @UnderTest
  @Singleton
  internal fun providesAff4ContainerUnderTest(
    @UnderTest aff4ContainerOpener: Aff4ContainerOpener,
    cleanupActions: GuiceExtension.CleanupActions,
    @ForImages imagesFileSystem: FileSystem,
  ): Aff4Container {
    val aff4Container = aff4ContainerOpener.open(imagesFileSystem, imageName.toPath())
    cleanupActions.register { aff4Container.close() }
    return aff4Container
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
