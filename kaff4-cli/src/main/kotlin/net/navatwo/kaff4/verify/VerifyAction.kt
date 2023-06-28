package net.navatwo.kaff4.verify

import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton
import misk.scope.ActionScope
import misk.scope.executor.ActionScopedExecutorService
import net.navatwo.kaff4.model.Aff4Image
import net.navatwo.kaff4.model.Aff4ImageOpener
import net.navatwo.kaff4.model.Aff4Model
import net.navatwo.kaff4.model.VerifiableStreamProvider
import net.navatwo.kaff4.model.query
import net.navatwo.kaff4.model.rdf.Aff4RdfBaseModels
import net.navatwo.kaff4.model.rdf.ImageStream
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.model.rdf.ZipSegment
import net.navatwo.logging.Logging
import okio.FileSystem
import okio.Path
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

private val logger = Logging.getLogger()

@Singleton
internal class VerifyAction @Inject constructor(
  private val imageOpener: Aff4ImageOpener,
  private val actionScopeProvider: Provider<ActionScope>,
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

      val executor = setupExecutorService(verifyThreadCount)

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

  private fun setupExecutorService(verifyThreadCount: Int): ExecutorService {
    val wrappedExecutor = Executors.newFixedThreadPool(
      verifyThreadCount,
      object : ThreadFactory {
        private var counter = 0

        @Synchronized
        override fun newThread(r: Runnable): Thread {
          return Thread(r, "VerifyWorker-${counter++}")
        }
      }
    )

    return ActionScopedExecutorService(wrappedExecutor, actionScopeProvider.get())
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
