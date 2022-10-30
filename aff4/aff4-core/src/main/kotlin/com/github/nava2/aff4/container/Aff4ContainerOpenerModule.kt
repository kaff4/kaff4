package com.github.nava2.aff4.container

import com.github.nava2.aff4.meta.rdf.ContainerArn
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.Aff4Container.ToolMetadata
import com.github.nava2.aff4.model.Aff4ContainerContext
import com.github.nava2.aff4.model.Aff4ContainerOpener
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4ModelModule
import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.model.Aff4StreamOpenerModule
import com.github.nava2.aff4.rdf.RdfExecutor
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.to
import com.google.inject.Provides
import okio.FileSystem
import org.eclipse.rdf4j.model.IRI

object Aff4ContainerOpenerModule : KAbstractModule() {
  override fun configure() {
    install(ContainerScopeModule())

    bind<Aff4ContainerOpener>().to<RealAff4ContainerOpener>()

    install(Aff4ModelModule)
    install(Aff4StreamOpenerModule)
  }

  @Provides
  @ContainerScoped
  internal fun provides(
    aff4ModelLoader: Aff4Model.Loader,
    @ContainerScoped aff4ContainerContext: Aff4ContainerContext,
  ): Aff4Model {
    return aff4ModelLoader.load(aff4ContainerContext)
  }

  @Provides
  @ContainerScoped
  internal fun providesContainer(
    aff4Model: Aff4Model,
    streamOpener: Aff4StreamOpener,
    @ContainerScoped containerMetadata: ToolMetadata,
  ): Aff4Container {
    return RealAff4Container(
      aff4Model = aff4Model,
      streamOpener = streamOpener,
      metadata = containerMetadata,
    )
  }

  @Provides
  @ContainerScoped
  internal fun providesContainerContext(
    rdfExecutor: RdfExecutor,
    @ContainerScoped @ContainerArn containerArn: IRI,
    @ContainerScoped containerMetadata: ToolMetadata,
    @ContainerScoped @ForImageRoot imageFileSystem: FileSystem,
  ) = Aff4ContainerContext(
    imageFileSystem = imageFileSystem,
    containerArn = containerArn,
    metadata = containerMetadata,
    rdfExecutor = rdfExecutor,
  )
}
