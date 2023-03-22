package com.github.nava2.aff4

import com.github.nava2.aff4.container.Aff4ImageOpenerModule
import com.github.nava2.aff4.model.Aff4Image
import com.github.nava2.aff4.model.Aff4ImageOpener
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.test.GuiceExtension
import com.google.inject.Provides
import okio.FileSystem
import okio.Path.Companion.toPath
import javax.inject.Inject
import javax.inject.Singleton

abstract class Aff4ImageTestModule(val imageName: String) : KAbstractModule() {
  val imagePath = imageName.toPath()

  override fun configure() {
    install(Aff4TestModule)

    install(Aff4ImageOpenerModule)
    install(Aff4CoreModule)
    install(Aff4BaseStreamModule)

    bindSet<GuiceExtension.TestLifecycleAction> {
      to<ImageTestLifecycleAction>()
    }

    configureOther()
  }

  protected open fun configureOther() = Unit

  @Provides
  @UnderTest
  @Singleton
  internal fun providesAff4ContainerUnderTest(imageTestLifecycleAction: ImageTestLifecycleAction): Aff4Image {
    return imageTestLifecycleAction.openImage(imageName)
  }

  @Provides
  @Singleton
  @UnderTest
  internal fun providesAff4ModelUnderTest(@UnderTest image: Aff4Image) = image.aff4Model

  @Provides
  @Singleton
  @UnderTest
  internal fun providesAff4StreamOpenerUnderTest(@UnderTest image: Aff4Image) = image.streamOpener

  @Singleton
  internal class ImageTestLifecycleAction @Inject constructor(
    private val aff4ImageOpener: Aff4ImageOpener,
    @ForImages private val imagesFileSystem: FileSystem,
  ) : GuiceExtension.TestLifecycleAction {

    private var imageWithResources: Aff4ImageOpener.Aff4ImageWithResources? = null

    fun openImage(imageName: String): Aff4Image {
      check(imageWithResources == null) { "Can not open multiple images" }

      check(imagesFileSystem.exists(imageName.toPath())) {
        "Image path does not exist: ${imagesFileSystem.canonicalize(imageName.toPath())}"
      }

      val openedImageWithResources = aff4ImageOpener.manualOpen(imagesFileSystem, imageName.toPath())
      imageWithResources = openedImageWithResources
      return openedImageWithResources.aff4Image
    }

    @Synchronized
    override fun afterEach() {
      imageWithResources?.close()
      imageWithResources = null
    }
  }

  companion object
}
