package com.github.nava2.aff4.container

import com.github.nava2.aff4.container.RealAff4ImageOpener.LoadedContainersContext
import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.google.common.annotations.VisibleForTesting
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject
import kotlin.io.path.name

private val AFF4_STRIPE_PATTERNS = listOf(
  Regex("[_.](?<index>\\d+)\\.aff4$"), // aff4-std: base-linear_1.aff4, base-linear.1.aff4
  Regex("\\.aff4\\.(?<index>\\d+)$"), // community standard: base-linear.aff4.000
)

internal class ContainerLoader @Inject constructor(
  private val valueFactory: ValueFactory,
) {
  fun getContainersForImage(fileSystem: FileSystem, openPath: Path): List<LoadedContainersContext.Container> {
    // if null, not striped
    val stripeIndex = maybeExtractStripeIndex(openPath.toNioPath().name)
      ?: return listOf(openContainer(fileSystem, openPath))

    val containerName = stripeIndex.containerName
    val stripedVolumePathsSorted = (openPath.parent?.let { fileSystem.list(it) } ?: listOf())
      .asSequence()
      .filter { it.toNioPath().name.startsWith(containerName) }
      .mapNotNull { p -> maybeExtractStripeIndex(p.toNioPath().name)?.let { p to it.index } }
      .sortedBy { (_, index) -> index }

    return stripedVolumePathsSorted
      .map { (p, _) -> openContainer(fileSystem, p) }
      .toList()
  }

  @VisibleForTesting
  internal fun maybeExtractStripeIndex(name: String): StripeIndex? {
    val match = AFF4_STRIPE_PATTERNS.firstNotNullOfOrNull { it.find(name) }

    return match?.let { m ->
      val containerName = name.substring(0 until m.range.first)
      val stripeIndex = m.groups["index"]!!.value.toInt()
      StripeIndex(containerName, stripeIndex)
    }
  }

  @VisibleForTesting
  internal data class StripeIndex(
    val containerName: String,
    val index: Int,
  )

  private fun openContainer(fileSystem: FileSystem, path: Path): LoadedContainersContext.Container {
    val dataFileSystem = openDataFileSystem(fileSystem, path)
    val containerArn = dataFileSystem.readContainerArn()
    val toolMetadata = Aff4Container.ToolMetadata.loadFromContainer(dataFileSystem)
    return LoadedContainersContext.Container(
      container = Aff4Container(
        containerArn = containerArn,
        dataFileSystem = dataFileSystem,
        metadata = toolMetadata,
      ),
      imageName = path.getContainerName(),
      containerFileSystem = fileSystem,
      containerPath = path,
    )
  }

  private fun openDataFileSystem(fileSystem: FileSystem, path: Path): FileSystem {
    val metadata = fileSystem.metadata(path)
    return if (metadata.isRegularFile) {
      fileSystem.openZip(path)
    } else {
      fileSystem.relativeTo(path)
    }
  }

  private fun FileSystem.readContainerArn(): Aff4Arn {
    return read("container.description".toPath()) {
      val arn = readString(Charsets.UTF_8).trimEnd()
      valueFactory.createIRI(arn)
    }
  }
}

private fun Path.getContainerName(): String {
  val lastSegment = segments.last()

  val match = AFF4_STRIPE_PATTERNS.firstNotNullOfOrNull { it.find(lastSegment) }
    ?: return lastSegment.substringBefore('.') // not striped

  return lastSegment.substring(0 until match.range.first)
}

private fun Aff4Container.ToolMetadata.Companion.loadFromContainer(
  imageFileSystem: FileSystem,
): Aff4Container.ToolMetadata {
  val (version, tool) = imageFileSystem.read("version.txt".toPath()) {
    var major = -1
    var minor = -1
    lateinit var tool: String
    for (line in readString(Charsets.UTF_8).lineSequence().filter { it.isNotEmpty() }) {
      val (tag, value) = line.split('=')
      when (tag) {
        "major" -> major = value.toInt()
        "minor" -> minor = value.toInt()
        "tool" -> tool = value
      }
    }

    check(major >= 0 && minor >= 0) { "Invalid version.txt" }
    "$major.$minor" to tool
  }

  return Aff4Container.ToolMetadata(version, tool)
}
