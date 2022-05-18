package com.github.nava2.aff4

import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4ModelModule
import com.github.nava2.aff4.streams.image_stream.Aff4ImageStreamModule
import com.github.nava2.aff4.streams.map_stream.Aff4MapStreamModule
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.typeLiteral
import com.github.nava2.test.GuiceTestRule
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.Provides
import com.google.inject.name.Names
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

class Aff4ImageTestRule(vararg modules: Module, val imageName: String = "Base-Linear.aff4") : GuiceTestRule(
  Aff4CoreModule,
  TestImagesModule,
  *modules,
) {
  override fun setupInjector(base: Statement, method: FrameworkMethod, target: Any, injector: Injector): Injector {
    val imagesFileSystem = injector.getInstance(Key.get(typeLiteral<FileSystem>(), ForImages::class.java))
    check(imagesFileSystem.exists(imageName.toPath())) {
      "Image path does not exist: ${imagesFileSystem.canonicalize(imageName.toPath())}"
    }

    return injector.createChildInjector(
      Aff4ModelModule,
      Aff4ImageStreamModule,
      Aff4MapStreamModule,
      object : KAbstractModule() {
        override fun configure() {
          bind<Aff4Model>().toProvider(Aff4ModelProvider::class.java).asEagerSingleton()

          bind<String>().annotatedWith(Names.named("test-image-name")).toInstance(imageName)
        }

        @Provides
        @Singleton
        @ForImageRoot
        fun providesImageRootFileSystem(aff4Model: Aff4Model) = aff4Model.imageRootFileSystem
      },
    )
  }

  private class Aff4ModelProvider @Inject constructor(
    @ForImages private val imagesFileSystem: FileSystem,
    private val aff4ModelLoader: Aff4Model.Loader,
    @Named("test-image-name") private val imageName: String,
  ) : Provider<Aff4Model> {
    override fun get(): Aff4Model = aff4ModelLoader.load(imagesFileSystem, imageName.toPath())
  }
}