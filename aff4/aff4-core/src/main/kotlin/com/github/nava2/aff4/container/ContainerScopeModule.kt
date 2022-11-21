package com.github.nava2.aff4.container

import com.github.nava2.aff4.meta.rdf.ContainerArn
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Container.ToolMetadata
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.key
import com.google.inject.Provides
import okio.FileSystem
import org.eclipse.rdf4j.model.IRI
import javax.inject.Named

class ContainerScopeModule : KAbstractModule() {
  private val containerScope = ContainerScope()

  override fun configure() {
    // tell Guice about the scope
    bindScope(ContainerScoped::class.java, containerScope)

    bind(key<FileSystem>(ForImageRoot::class))
      .toProvider(ContainerScope.seededKeyProvider())
      .`in`(ContainerScoped::class.java)
    bind(key<IRI>(ContainerArn::class))
      .toProvider(ContainerScope.seededKeyProvider())
      .`in`(ContainerScoped::class.java)
    bind(key<ToolMetadata>())
      .toProvider(ContainerScope.seededKeyProvider())
      .`in`(ContainerScoped::class.java)
  }

  @Provides
  @Named("containerScope")
  internal fun provideContainerScope(): ContainerScope = containerScope
}
