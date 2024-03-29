package net.navatwo.kaff4.container

import jakarta.inject.Inject
import misk.scope.ActionScope
import misk.scope.ActionScoped
import net.navatwo.guice.key
import net.navatwo.kaff4.model.Aff4Container
import net.navatwo.kaff4.model.Aff4Image
import net.navatwo.kaff4.model.Aff4ImageOpener
import net.navatwo.kaff4.model.Aff4ImageOpener.Aff4ImageWithResources
import okio.FileSystem
import okio.Path

internal class RealAff4ImageOpener @Inject constructor(
  private val actionScope: ActionScope,
  private val containerLoader: ContainerLoader,
  private val imageProvider: ActionScoped<Aff4Image>,
) : Aff4ImageOpener {
  override fun manualOpen(fileSystem: FileSystem, path: Path): Aff4ImageWithResources {
    val containers = containerLoader.getContainersForImage(fileSystem, path)
    check(containers.isNotEmpty()) {
      "Failed to load image [$path]: no containers found."
    }

    val openedContainer = containers.single { it.containerPath == path }

    val action = actionScope.enter(
      mapOf(
        key<LoadedContainersContext>() to LoadedContainersContext(imageName = openedContainer.imageName, containers),
      )
    )

    val aff4Image = imageProvider.get()
    return Aff4ImageWithResources(aff4Image) { action.close() }
  }

  data class LoadedContainersContext(
    val imageName: String,
    val containers: List<Container>,
  ) {
    data class Container(
      val imageName: String,
      val container: Aff4Container,
      val containerFileSystem: FileSystem,
      val containerPath: Path,
    )
  }
}
