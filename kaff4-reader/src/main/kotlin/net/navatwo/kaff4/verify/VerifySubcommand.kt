package net.navatwo.kaff4.verify

import com.google.inject.Guice
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.vararg
import net.navatwo.guice.getInstance
import net.navatwo.kaff4.Aff4BaseStreamModule
import net.navatwo.kaff4.Aff4CoreModule
import net.navatwo.kaff4.Aff4LogicalModule
import net.navatwo.kaff4.RandomsModule
import net.navatwo.kaff4.container.Aff4ImageOpenerModule
import net.navatwo.kaff4.rdf.MemoryRdfRepositoryPlugin
import net.navatwo.kaff4.streams.compression.Aff4SnappyPlugin
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

private const val DEFAULT_THREAD_COUNT = 8

@ExperimentalCli
class VerifySubcommand : Subcommand("verify", "Verify an image") {
  private val inputImagesArg by argument(
    type = ArgType.String,
    fullName = "input_file",
    description = "Input image to verify"
  ).vararg()

  private val verifyThreadCount by option(
    type = ArgType.Int,
    shortName = "n",
    fullName = "thread_count",
    description = "Number of threads to use for verification"
  ).default(DEFAULT_THREAD_COUNT)

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
    val injector = Guice.createInjector(
      RandomsModule,
      MemoryRdfRepositoryPlugin,
      Aff4ImageOpenerModule,
      Aff4CoreModule,
      Aff4BaseStreamModule,
      Aff4LogicalModule,
      Aff4SnappyPlugin,
    )

    val verifyAction = injector.getInstance<VerifyAction>()
    verifyAction.execute(inputImages, verifyThreadCount)
  }
}
