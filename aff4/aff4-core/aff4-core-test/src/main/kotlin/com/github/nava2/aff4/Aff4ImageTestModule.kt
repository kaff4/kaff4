package com.github.nava2.aff4

import com.github.nava2.aff4.container.Aff4ImageOpenerModule
import com.github.nava2.aff4.model.Aff4Image
import com.github.nava2.aff4.model.Aff4ImageOpener
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.test.GuiceExtension
import com.google.inject.Provides
import okio.FileSystem
import okio.Path.Companion.toPath
import javax.inject.Singleton

class Aff4ImageTestModule(private val imageName: String) : KAbstractModule() {
  override fun configure() {
    install(Aff4TestModule)

    install(Aff4ImageOpenerModule)
    install(Aff4CoreModule)
    install(Aff4BaseStreamModule)
  }

  @Provides
  @UnderTest
  @Singleton
  internal fun providesAff4ContainerUnderTest(
    aff4ImageOpener: Aff4ImageOpener,
    cleanupActions: GuiceExtension.CleanupActions,
    @ForImages imagesFileSystem: FileSystem,
  ): Aff4Image {
    check(imagesFileSystem.exists(imageName.toPath())) {
      "Image path does not exist: ${imagesFileSystem.canonicalize(imageName.toPath())}"
    }

    return aff4ImageOpener.manualOpen(imagesFileSystem, imageName.toPath())
      .apply(cleanupActions::register)
      .aff4Image
  }

  @Provides
  @Singleton
  @UnderTest
  internal fun providesAff4ModelUnderTest(@UnderTest aff4Image: Aff4Image) = aff4Image.aff4Model

  @Provides
  @Singleton
  @UnderTest
  internal fun providesAff4StreamOpenerUnderTest(@UnderTest aff4Image: Aff4Image) = aff4Image.streamOpener

  companion object
}
