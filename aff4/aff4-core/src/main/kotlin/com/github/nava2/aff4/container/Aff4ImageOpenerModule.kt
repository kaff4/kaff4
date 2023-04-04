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
import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.guice.action_scoped.ActionScoped
import javax.inject.Inject
import javax.inject.Provider

object Aff4ImageOpenerModule : KAff4AbstractModule() {
  override fun configure() {
    install(ImageScopeModule)

    install(Aff4ModelModule)
    install(Aff4StreamOpenerModule)

    bind<Aff4ImageOpener>().to<RealAff4ImageOpener>()

    bind<Aff4Model>()
      .toProvider(Aff4ModelActionScopedProvider::class.java)
      .`in`(ActionScoped::class.java)

    bind<Aff4Image>()
      .toProvider(Aff4ImageActionScopedProvider::class.java)
      .`in`(ActionScoped::class.java)

    bind<Aff4ImageContext>()
      .toProvider(Aff4ImageContextActionScopedProvider::class.java)
      .`in`(ActionScoped::class.java)

    bind<ToolDialect>()
      .toProvider(Aff4ToolDialectActionScopedProvider::class.java)
      .`in`(ActionScoped::class.java)
  }

  private class Aff4ImageActionScopedProvider @Inject constructor(
    private val aff4ModelProvider: Provider<Aff4Model>,
    private val streamOpener: Aff4StreamOpener,
    private val loadedContainersContextProvider: Provider<RealAff4ImageOpener.LoadedContainersContext>,
  ) : Provider<Aff4Image> {
    override fun get() = RealAff4Image(
      aff4Model = aff4ModelProvider.get(),
      streamOpener = streamOpener,
      containers = loadedContainersContextProvider.get().containers.map { it.container },
    )
  }

  private class Aff4ImageContextActionScopedProvider @Inject constructor(
    private val rdfExecutor: RdfExecutor,
    private val loadedContainersContextProvider: Provider<RealAff4ImageOpener.LoadedContainersContext>,
  ) : Provider<Aff4ImageContext> {
    override fun get(): Aff4ImageContext {
      val loadedContainersContext = loadedContainersContextProvider.get()
      return Aff4ImageContext(
        imageName = loadedContainersContext.imageName,
        rdfExecutor = rdfExecutor,
        containers = loadedContainersContext.containers.map { it.container },
      )
    }
  }

  private class Aff4ModelActionScopedProvider @Inject constructor(
    private val aff4ModelLoader: Aff4Model.Loader,
    private val aff4ImageContextProvider: Provider<Aff4ImageContext>,
  ) : Provider<Aff4Model> {
    override fun get() = aff4ModelLoader.load(aff4ImageContextProvider.get())
  }

  private class Aff4ToolDialectActionScopedProvider @Inject constructor(
    private val loadedContainersContextProvider: Provider<RealAff4ImageOpener.LoadedContainersContext>,
    private val toolDialectResolver: ToolDialectResolver,
  ) : Provider<ToolDialect> {
    override fun get(): ToolDialect {
      val loadedContainersContext = loadedContainersContextProvider.get()
      val toolMetadata = loadedContainersContext.containers.asSequence()
        .map { it.container.metadata }
        .distinct()
        .single()

      return toolDialectResolver.forTool(toolMetadata)
    }
  }
}
