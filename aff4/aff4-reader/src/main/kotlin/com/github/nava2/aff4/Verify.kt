package com.github.nava2.aff4

import com.github.nava2.aff4.container.Aff4ImageOpenerModule
import com.github.nava2.aff4.model.Aff4ImageOpener
import com.github.nava2.aff4.model.Aff4ImageOpenerFactory
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.query
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryPlugin
import com.github.nava2.aff4.streams.compression.Aff4SnappyPlugin
import com.github.nava2.logging.Logging
import kotlinx.cli.ArgType
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
    val aff4ImageOpenerFactory = Aff4ImageOpenerFactory(
      RandomsModule,
      MemoryRdfRepositoryPlugin,
      Aff4ImageOpenerModule,
      Aff4CoreModule,
      Aff4BaseStreamModule,
      Aff4LogicalModule,
      Aff4SnappyPlugin,
    )

    logger.info("Verifying ${inputImages.size} images: $inputImages")
    for (imagePath in inputImages) {
      val aff4ImageOpener = aff4ImageOpenerFactory.create()

      verifyImage(aff4ImageOpener, imagePath)
    }
  }

  private fun verifyImage(aff4ImageOpener: Aff4ImageOpener, imagePath: Path) {
    logger.info("Opening image: $imagePath")

    aff4ImageOpener.open(FileSystem.SYSTEM, imagePath) { container ->
      logger.debug("Opened image, querying streams")

      val model = container.aff4Model
      val streams = model.query<ImageStream>() + model.query<MapStream>() + model.query<ZipSegment>()

      logger.info("Verifying ${streams.size} streams")
      logger.debug("Verifying [${streams.size}] = [\n\t${streams.joinToString("\n\t")}\n]")

      for (stream in streams) {
        val streamOpener = container.streamOpener.openStream(stream.arn)
        if (streamOpener !is VerifiableStreamProvider) continue

        val result = streamOpener.verify(model)
        check(result !is VerifiableStreamProvider.Result.Failed) {
          "Failed to verify: arn=${stream.arn}, Failed Hashes: ${result.failedHashes}"
        }

        logger.info("Verified stream: arn=${stream.arn}")
      }
    }

    logger.info("Verified image: $imagePath")
  }
}
