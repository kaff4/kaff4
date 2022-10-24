@file:OptIn(ExperimentalCli::class)

package com.github.nava2.aff4

import com.github.nava2.aff4.container.Aff4ContainerOpenerBuilder
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.query
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryModule
import com.github.nava2.aff4.streams.compression.Aff4SnappyModule
import com.github.nava2.guice.GuiceFactory
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.getInstance
import com.github.nava2.logging.Logging
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.vararg
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class Verify : Subcommand("verify", "Verify an image") {
  private val logger = Logging.getLogger()

  private val inputImagesArg by argument(
    ArgType.String,
    fullName = "input_file",
    description = "Input image to verify"
  ).vararg()

  private val inputImages: List<Path> by lazy {
    val inputImages = inputImagesArg.map { it.toPath() }
    for (inputImage in inputImages) {
      check(FileSystem.SYSTEM.exists(inputImage)) {
        "Input image does not exist: $inputImage"
      }
    }

    inputImages
  }

  override fun execute() {
    val modules = listOf(
      Aff4CoreModule,
      MemoryRdfRepositoryModule,
      object : KAbstractModule() {
        override fun configure() {
          bind<GuiceFactory>().toInstance(ChildInjectorGuiceFactory)
        }
      }
    )

    val injector = Guice.createInjector(Stage.DEVELOPMENT, modules)
    val aff4ContainerOpenerBuilder = injector.getInstance<Aff4ContainerOpenerBuilder>()

    logger.info("Verifying two images: $inputImages")
    for (imagePath in inputImages) {
      verifyImage(aff4ContainerOpenerBuilder, imagePath)
    }
  }

  private fun verifyImage(aff4ContainerOpenerBuilder: Aff4ContainerOpenerBuilder, imagePath: Path) {
    val aff4ContainerOpener = aff4ContainerOpenerBuilder
      .withFeatureModules(
        Aff4SnappyModule,
        MemoryRdfRepositoryModule,
      )
      .build()

    logger.info("Opening image: $imagePath")

    aff4ContainerOpener.open(FileSystem.SYSTEM, imagePath).use { container ->
      logger.debug("Opened image, querying streams")

      val model = container.aff4Model
      val streams = model.query<ImageStream>() + model.query<MapStream>() + model.query<ZipSegment>()

      logger.info("Verifying ${streams.size} streams")
      logger.debug("Verifying [${streams.size}] = [\n\t${streams.joinToString("\n\t")}\n]")

      for (stream in streams) {
        val streamOpener = container.streamOpener.openStream(stream.arn)
        streamOpener as? VerifiableStreamProvider ?: continue

        val result = streamOpener.verify(model)
        check(result !is VerifiableStreamProvider.Result.Failed) {
          "Failed to verify: arn=${stream.arn}, Failed Hashes: ${result.failedHashes}"
        }

        logger.info("Verified stream: arn=${stream.arn}")
      }
    }

    logger.info("Verified image: $imagePath")

    logger.warn("foo bar")
    logger.error("baz")
  }
}

fun main(args: Array<String>) {
  val parser = ArgParser("kaff4-cli", strictSubcommandOptionsOrder = true)

  val verify = Verify()
  parser.subcommands(verify)

  parser.parse(args)
}

private object ChildInjectorGuiceFactory : GuiceFactory {
  override fun create(modules: Collection<Module>): Injector {
    return Guice.createInjector(Stage.DEVELOPMENT, modules)
  }
}
