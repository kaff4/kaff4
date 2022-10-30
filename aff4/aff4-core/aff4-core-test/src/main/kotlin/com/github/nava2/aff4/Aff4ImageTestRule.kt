package com.github.nava2.aff4

import com.github.nava2.aff4.container.Aff4ContainerOpenerModule
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.Aff4ContainerOpener
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryModule
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.key
import com.github.nava2.test.GuiceTestRule
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Provides
import okio.FileSystem
import okio.Path.Companion.toPath
import javax.inject.Singleton

open class Aff4ImageTestRule(val imageName: String, vararg modules: Module) : GuiceTestRule(
  providedModules = (
    modules.toList() +
      listOf(
        Aff4ContainerOpenerModule,
        Aff4CoreModule,
        Aff4BaseStreamModule,
        MemoryRdfRepositoryModule,
        TestRandomsModule,
        UnderTestModule(imageName),
      )
    ).toTypedArray()
) {

  override fun evaluate(injector: Injector, block: () -> Unit) {
    val imagesFileSystem = injector.getInstance(key<FileSystem>(ForImages::class))
    check(imagesFileSystem.exists(imageName.toPath())) {
      "Image path does not exist: ${imagesFileSystem.canonicalize(imageName.toPath())}"
    }

    block()
  }

  private class UnderTestModule(
    val imageName: String
  ) : KAbstractModule() {
    override fun configure() {
      install(TestImagesModule)
    }

    @Provides
    @UnderTest
    @Singleton
    fun providesAff4ContainerUnderTest(
      @ForImages imagesFileSystem: FileSystem,
      aff4ContainerOpener: Aff4ContainerOpener,
      cleanupActions: CleanupActions,
    ): Aff4Container {
      return aff4ContainerOpener.manualOpen(imagesFileSystem, imageName.toPath())
        .apply(cleanupActions::register)
        .aff4Container
    }

    @Provides
    @Singleton
    @UnderTest
    fun providesAff4ModelUnderTest(@UnderTest aff4Container: Aff4Container) = aff4Container.aff4Model

    @Provides
    @Singleton
    @UnderTest
    fun providesAff4StreamOpenerUnderTest(@UnderTest aff4Container: Aff4Container) = aff4Container.streamOpener
  }
}
