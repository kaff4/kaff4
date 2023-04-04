package net.navatwo.kaff4.container

import misk.scope.ActionScoped
import misk.scope.ActionScopedProvider
import misk.scope.ActionScopedProviderModule
import net.navatwo.guice.KAff4AbstractModule
import net.navatwo.kaff4.model.Aff4Image
import net.navatwo.kaff4.model.Aff4ImageContext
import net.navatwo.kaff4.model.Aff4ImageOpener
import net.navatwo.kaff4.model.Aff4Model
import net.navatwo.kaff4.model.Aff4ModelModule
import net.navatwo.kaff4.model.Aff4StreamOpener
import net.navatwo.kaff4.model.Aff4StreamOpenerModule
import net.navatwo.kaff4.model.dialect.ToolDialect
import net.navatwo.kaff4.rdf.RdfExecutor
import javax.inject.Inject

object Aff4ImageOpenerModule : KAff4AbstractModule() {
  override fun configure() {
    install(Aff4ModelModule)
    install(Aff4StreamOpenerModule)

    bind<Aff4ImageOpener>().to<RealAff4ImageOpener>()

    install(
      object : ActionScopedProviderModule() {
        override fun configureProviders() {
          bindSeedData(RealAff4ImageOpener.LoadedContainersContext::class)

          bindProvider(Aff4Model::class, Aff4ModelActionScopedProvider::class)
          bindProvider(Aff4Image::class, Aff4ImageActionScopedProvider::class)
          bindProvider(Aff4ImageContext::class, Aff4ImageContextActionScopedProvider::class)
          bindProvider(ToolDialect::class, Aff4ToolDialectActionScopedProvider::class)
        }
      }
    )
  }

  private class Aff4ImageActionScopedProvider @Inject constructor(
    private val aff4ModelProvider: ActionScoped<Aff4Model>,
    private val streamOpener: Aff4StreamOpener,
    private val loadedContainersContextProvider: ActionScoped<RealAff4ImageOpener.LoadedContainersContext>,
  ) : ActionScopedProvider<Aff4Image> {
    override fun get() = RealAff4Image(
      aff4Model = aff4ModelProvider.get(),
      streamOpener = streamOpener,
      containers = loadedContainersContextProvider.get().containers.map { it.container },
    )
  }

  private class Aff4ImageContextActionScopedProvider @Inject constructor(
    private val rdfExecutor: RdfExecutor,
    private val loadedContainersContextProvider: ActionScoped<RealAff4ImageOpener.LoadedContainersContext>,
  ) : ActionScopedProvider<Aff4ImageContext> {
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
    private val aff4ImageContextProvider: ActionScoped<Aff4ImageContext>,
  ) : ActionScopedProvider<Aff4Model> {
    override fun get() = aff4ModelLoader.load(aff4ImageContextProvider.get())
  }

  private class Aff4ToolDialectActionScopedProvider @Inject constructor(
    private val loadedContainersContextProvider: ActionScoped<RealAff4ImageOpener.LoadedContainersContext>,
    private val toolDialectResolver: ToolDialectResolver,
  ) : ActionScopedProvider<ToolDialect> {
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
