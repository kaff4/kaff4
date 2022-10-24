package com.github.nava2.aff4.container

import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.model.Aff4Container
import com.github.nava2.aff4.model.Aff4Container.ToolMetadata
import com.github.nava2.aff4.model.Aff4ContainerContext
import com.github.nava2.aff4.model.Aff4ContainerOpener
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4ModelModule
import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.model.Aff4StreamOpenerModule
import com.github.nava2.aff4.rdf.RdfConnectionScoping
import com.github.nava2.aff4.streams.image_stream.Aff4ImageStreamModule
import com.github.nava2.aff4.streams.map_stream.Aff4MapStreamModule
import com.github.nava2.aff4.streams.zip_segment.Aff4ZipSegmentModule
import com.github.nava2.guice.GuiceFactory
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.getInstance
import com.github.nava2.guice.to
import com.google.common.annotations.VisibleForTesting
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Provides
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import org.eclipse.rdf4j.model.impl.SimpleIRI
import javax.inject.Singleton

class RealAff4ContainerOpener internal constructor(
  private val guiceFactory: GuiceFactory,
  private val featureModules: Set<Module>,
) : Aff4ContainerOpener {
  override fun open(fileSystem: FileSystem, path: Path): Aff4Container {
    val childInjector = setupContainerInjector(fileSystem, path, setOf())
    return childInjector.getInstance()
  }

  @VisibleForTesting
  fun setupContainerInjector(fileSystem: FileSystem, path: Path, extraModules: Set<Module>): Injector {
    val imageFileSystem = openImageFileSystem(fileSystem, path)

    val containerMetadata = ToolMetadata.loadFromImage(imageFileSystem)

    val containerArn = imageFileSystem.read("container.description".toPath()) {
      val arn = readString(Charsets.UTF_8).trimEnd()
      object : SimpleIRI(arn) {}
    }

    val modules = containerMetadata.streamModules() +
      featureModules +
      extraModules +
      ContainerContextModule(imageFileSystem, containerArn, containerMetadata)

    return guiceFactory.create(modules)
  }

  private fun openImageFileSystem(fileSystem: FileSystem, path: Path): FileSystem {
    return if (path.toFile().extension in setOf("zip", "aff4")) {
      fileSystem.openZip(path)
    } else {
      fileSystem.relativeTo(path)
    }
  }

  private class ContainerContextModule(
    private val imageFileSystem: FileSystem,
    private val containerArn: SimpleIRI,
    private val containerMetadata: ToolMetadata
  ) : KAbstractModule() {
    override fun configure() {
      bind<Aff4Container>().to<RealAff4Container>()

      bind<FileSystem>()
        .annotatedWith(ForImageRoot::class.java)
        .toInstance(imageFileSystem)

      install(Aff4ModelModule)
      install(Aff4StreamOpenerModule)
    }

    @Provides
    @Singleton
    fun provides(aff4ModelLoader: Aff4Model.Loader, aff4ContainerContext: Aff4ContainerContext): Aff4Model {
      return aff4ModelLoader.load(aff4ContainerContext)
    }

    @Provides
    @Singleton
    fun providesContainer(
      aff4Model: Aff4Model,
      streamOpener: Aff4StreamOpener,
    ): RealAff4Container {
      return RealAff4Container(
        aff4Model = aff4Model,
        streamOpener = streamOpener,
        metadata = containerMetadata,
      )
    }

    @Provides
    fun providesContainerContext(
      rdfConnectionScoping: RdfConnectionScoping,
    ) = Aff4ContainerContext(
      imageFileSystem = imageFileSystem,
      containerArn = containerArn,
      metadata = containerMetadata,
      rdfConnectionScoping = rdfConnectionScoping,
    )
  }
}

private fun ToolMetadata.streamModules(): Set<Module> = when (version) {
  "1.0" -> setOf(
    Aff4ImageStreamModule,
    Aff4MapStreamModule,
  )

  "1.1" -> setOf(
    Aff4ImageStreamModule,
    Aff4MapStreamModule,
    Aff4ZipSegmentModule,
  )

  else -> error("Unsupported AFF4 version")
}

private fun ToolMetadata.Companion.loadFromImage(imageFileSystem: FileSystem): ToolMetadata {
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

  return ToolMetadata(version, tool)
}
