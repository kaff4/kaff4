package com.github.nava2.aff4

import com.github.nava2.aff4.container.Aff4ContainerOpenerBuilder
import com.github.nava2.aff4.container.RealAff4ContainerOpener
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.Aff4ContainerOpener
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.getInstance
import com.github.nava2.guice.typeLiteral
import com.github.nava2.test.GuiceTestRule
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.Provides
import okio.FileSystem
import okio.Path.Companion.toPath
import javax.inject.Singleton

open class Aff4ImageTestRule(val imageName: String, vararg modules: Module) : GuiceTestRule(
  providedModules = listOf(TestImagesModule),
) {
  private val providedModules = modules.toSet()

  open val imageModules: List<Module> = listOf()

  override fun setupInjector(injector: Injector, cleanupActions: CleanupActions): Injector {
    val imagesFileSystem = injector.getInstance(Key.get(typeLiteral<FileSystem>(), ForImages::class.java))
    check(imagesFileSystem.exists(imageName.toPath())) {
      "Image path does not exist: ${imagesFileSystem.canonicalize(imageName.toPath())}"
    }

    val aff4ContainerOpenerBuilder = injector.getInstance<Aff4ContainerOpenerBuilder>()

    val aff4ContainerOpener = aff4ContainerOpenerBuilder
      .withFeatureModules(providedModules + imageModules)
      .build() as RealAff4ContainerOpener

    val testInjector = aff4ContainerOpener.setupContainerInjector(
      fileSystem = imagesFileSystem,
      path = imageName.toPath(),
      extraModules = setOf(UnderTestModule(aff4ContainerOpener, imageName)),
    )

    cleanupActions.register {
      testInjector.getInstance(Key.get(Aff4Container::class.java, UnderTest::class.java)).close()
    }

    return testInjector
  }

  private class UnderTestModule(
    val aff4ContainerOpener: Aff4ContainerOpener,
    val imageName: String
  ) : KAbstractModule() {
    override fun configure() = Unit

    @Provides
    @UnderTest
    @Singleton
    fun providesAff4ContainerUnderTest(
      @ForImages imagesFileSystem: FileSystem,
    ): Aff4Container {
      return aff4ContainerOpener.open(imagesFileSystem, imageName.toPath())
    }

    @Provides
    @Singleton
    @UnderTest
    fun providesAff4ModelUnderTest(@UnderTest aff4Container: Aff4Container) = aff4Container.aff4Model
  }
}
