package com.github.nava2.aff4

import com.github.nava2.aff4.container.Aff4ImageOpenerModule
import com.github.nava2.aff4.model.Aff4ImageOpener
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.query
import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryPlugin
import com.github.nava2.aff4.streams.compression.Aff4SnappyPlugin
import com.github.nava2.guice.action_scoped.ActionScopedExecutors
import com.github.nava2.guice.getInstance
import com.github.nava2.logging.Logging
import com.google.inject.Guice
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.vararg
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.util.concurrent.Callable
import java.util.concurrent.ThreadFactory
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val DEFAULT_THREAD_COUNT = 8

private val logger = Logging.getLogger()

@ExperimentalCli
class Verify : Subcommand("verify", "Verify an image") {
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

    val action = injector.getInstance<Action>()
    action.execute(this)
  }

  @Singleton
  private class Action @Inject constructor(
    private val imageOpener: Aff4ImageOpener,
    private val executorsProvider: Provider<ActionScopedExecutors>,
  ) {
    fun execute(verify: Verify) {
      val inputImages = verify.inputImages

      logger.info("Verifying ${inputImages.size} images: $inputImages")
      for (imagePath in inputImages) {
        verifyImage(imageOpener, verify.verifyThreadCount, imagePath)
      }
    }

    private fun verifyImage(aff4ImageOpener: Aff4ImageOpener, verifyThreadCount: Int, imagePath: Path) {
      logger.info("Opening image: $imagePath")

      aff4ImageOpener.open(FileSystem.SYSTEM, imagePath) { container ->
        logger.debug("Opened image, querying streams")

        val model = container.aff4Model
        val streams = (model.query<ImageStream>() + model.query<MapStream>() + model.query<ZipSegment>()).toList()

        logger.info("Verifying ${streams.size} streams")
        logger.debug("Verifying [${streams.size}] = [\n\t${streams.joinToString("\n\t")}\n]")

        val executor = executorsProvider.get().newFixedThreadPool(
          nThreads = verifyThreadCount,
          threadFactory = object : ThreadFactory {
            private var counter = 0

            @Synchronized
            override fun newThread(r: Runnable): Thread {
              return Thread(r, "VerifyWorker-${counter++}")
            }
          }
        )

        try {
          val tasks = streams.map { stream ->
            Callable {
              val streamOpener = container.streamOpener.openStream(stream.arn)

              if (streamOpener !is VerifiableStreamProvider) return@Callable Result.Unsupported(stream.arn)

              when (val result = streamOpener.verify(model)) {
                is VerifiableStreamProvider.Result.Failed -> Result.Failure(stream.arn, result.failedHashes)
                VerifiableStreamProvider.Result.Success -> Result.Success(stream.arn)
              }
            }
          }

          val results = executor.invokeAll(tasks).map { it.get() }

          val verifiedImageStreams = results.filterIsInstance<Result.Success>().map { it.arn }
          logger.info("Verified image streams: $verifiedImageStreams")
          val unsupportedImageStreams = results.filterIsInstance<Result.Unsupported>().map { it.arn }
          if (unsupportedImageStreams.isNotEmpty()) {
            logger.error("Unsupported images: $unsupportedImageStreams")
          }

          for (result in results.filterIsInstance<Result.Failure>()) {
            logger.error("Failed to verify ${result.arn}: ${result.failedHashes}")
          }
        } finally {
          executor.shutdown()
        }
      }

      logger.info("Verified image: $imagePath")
    }
  }
}

private sealed interface Result {
  val arn: Aff4Arn

  data class Success(override val arn: Aff4Arn) : Result

  data class Failure(
    override val arn: Aff4Arn,
    val failedHashes: Collection<VerifiableStreamProvider.Result.FailedHash>,
  ) : Result

  data class Unsupported(override val arn: Aff4Arn) : Result
}
