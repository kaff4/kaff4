package com.github.nava2.aff4.verify

import com.github.nava2.aff4.model.Aff4Image
import com.github.nava2.aff4.model.Aff4ImageOpener
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.query
import com.github.nava2.aff4.model.rdf.Aff4RdfBaseModels
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.guice.action_scoped.ActionScopedExecutors
import com.github.nava2.logging.Logging
import okio.FileSystem
import okio.Path
import java.util.concurrent.Callable
import java.util.concurrent.ThreadFactory
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private val logger = Logging.getLogger()

@Singleton
internal class VerifyAction @Inject constructor(
  private val imageOpener: Aff4ImageOpener,
  private val executorsProvider: Provider<ActionScopedExecutors>,
) {
  fun execute(inputImages: List<Path>, verifyThreadCount: Int) {
    logger.info("Verifying ${inputImages.size} images: $inputImages")
    for (imagePath in inputImages) {
      verifyImage(imageOpener, verifyThreadCount, imagePath)
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
          Callable { verifyStream(container, stream, model) }
        }

        val results = executor.invokeAll(tasks).map { it.get() }

        val verifiedImageStreams = results.filterIsInstance<VerificationResult.Success>().map { it.arn }
        logger.info("Verified image streams: $verifiedImageStreams")
        val unsupportedImageStreams = results.filterIsInstance<VerificationResult.Unsupported>().map { it.arn }
        if (unsupportedImageStreams.isNotEmpty()) {
          logger.error("Unsupported images: $unsupportedImageStreams")
        }

        for (result in results.filterIsInstance<VerificationResult.Failure>()) {
          logger.error("Failed to verify ${result.arn}: ${result.failedHashes}")
        }
      } finally {
        executor.shutdown()
      }
    }

    logger.info("Verified image: $imagePath")
  }

  private fun verifyStream(
    container: Aff4Image,
    stream: Aff4RdfBaseModels,
    model: Aff4Model
  ): VerificationResult {
    return container.streamOpener.openStream(stream.arn).use { streamProvider ->
      if (streamProvider !is VerifiableStreamProvider) return@use VerificationResult.Unsupported(stream.arn)

      when (val result = streamProvider.verify(model)) {
        is VerifiableStreamProvider.Result.Failed -> VerificationResult.Failure(stream.arn, result.failedHashes)
        VerifiableStreamProvider.Result.Success -> VerificationResult.Success(stream.arn)
      }
    }
  }
}
