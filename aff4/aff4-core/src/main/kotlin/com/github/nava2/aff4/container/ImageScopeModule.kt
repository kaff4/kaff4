package com.github.nava2.aff4.container

import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.key
import com.google.inject.Provides
import javax.inject.Named

class ImageScopeModule : KAbstractModule() {
  private val imageScope = ImageScope()

  override fun configure() {
    // tell Guice about the scope
    bindScope(ImageScoped::class.java, imageScope)

    bind(key<RealAff4ImageOpener.LoadedContainersContext>())
      .toProvider(ImageScope.seededKeyProvider())
      .`in`(ImageScoped::class.java)
  }

  @Provides
  @Named("imageScope")
  internal fun provideImageScope(): ImageScope = imageScope
}
