package com.github.nava2.aff4.container

import com.github.nava2.aff4.model.Aff4Image
import com.github.nava2.aff4.model.Aff4ImageContext
import com.github.nava2.aff4.model.Aff4ImageOpener
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4ModelModule
import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.model.Aff4StreamOpenerModule
import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.rdf.RdfExecutor
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.action_scoped.ActionScoped
import com.github.nava2.guice.to
import com.google.inject.Provides

object Aff4ImageOpenerModule : KAbstractModule() {
  override fun configure() {
    install(ImageScopeModule)

    install(Aff4ModelModule)
    install(Aff4StreamOpenerModule)

    bind<Aff4ImageOpener>().to<RealAff4ImageOpener>()
  }

  @Provides
  @ActionScoped
  internal fun providesAff4Model(
    aff4ModelLoader: Aff4Model.Loader,
    @ActionScoped aff4ImageContext: Aff4ImageContext,
  ): Aff4Model {
    return aff4ModelLoader.load(aff4ImageContext)
  }

  @Provides
  @ActionScoped
  internal fun providesAff4Image(
    aff4Model: Aff4Model,
    streamOpener: Aff4StreamOpener,
    @ActionScoped loadedContainersContext: RealAff4ImageOpener.LoadedContainersContext,
  ): Aff4Image {
    return RealAff4Image(
      aff4Model = aff4Model,
      streamOpener = streamOpener,
      containers = loadedContainersContext.containers.map { it.container },
    )
  }

  @Provides
  @ActionScoped
  internal fun providesImageContext(
    rdfExecutor: RdfExecutor,
    @ActionScoped loadedContainersContext: RealAff4ImageOpener.LoadedContainersContext,
  ) = Aff4ImageContext(
    imageName = loadedContainersContext.imageName,
    rdfExecutor = rdfExecutor,
    containers = loadedContainersContext.containers.map { it.container },
  )

  @Provides
  @ActionScoped
  internal fun providesToolDialect(
    @ActionScoped loadedContainersContext: RealAff4ImageOpener.LoadedContainersContext,
    toolDialectResolver: ToolDialectResolver,
  ): ToolDialect {
    val toolMetadata = loadedContainersContext.containers.asSequence()
      .map { it.container.metadata }
      .distinct()
      .single()

    return toolDialectResolver.forTool(toolMetadata)
  }
}
