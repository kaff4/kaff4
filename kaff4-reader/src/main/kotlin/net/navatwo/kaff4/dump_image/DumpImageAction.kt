package net.navatwo.kaff4.dump_image

import com.google.common.base.Stopwatch
import net.navatwo.kaff4.io.ProgressSink
import net.navatwo.kaff4.io.TeeSink
import net.navatwo.kaff4.io.buffer
import net.navatwo.kaff4.model.Aff4ImageOpener
import net.navatwo.kaff4.model.Aff4StreamSourceProvider
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.logging.Logging
import okio.BufferedSource
import okio.FileSystem
import okio.GzipSink
import okio.Path
import okio.Sink
import okio.Timeout
import okio.sink
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val logger = Logging.getLogger()

@Singleton
internal class DumpImageAction @Inject constructor(
  private val imageOpener: Aff4ImageOpener,
) {
  enum class OutputFormat {
    BIN,
    GZIP,
    ;
  }

  fun execute(
    imagePath: Path,
    streamIdentifier: Aff4Arn,
    outputFormat: OutputFormat,
    dumpStandardOut: Boolean,
    outputFile: Path?,
  ) {
    openOutputSinks(outputFormat, dumpStandardOut, outputFile).use { outputSink ->
      imageOpener.open(FileSystem.SYSTEM, imagePath) { container ->
        logger.debug("Opened image, querying streams")

        val streamOpener = container.streamOpener
        streamOpener.openStream(streamIdentifier).use { sourceProvider ->
          val sourceSize = (sourceProvider as? Aff4StreamSourceProvider)?.size?.toBigDecimal()

          // TODO Make this do the things we need, compute md5/sha1, dump to `outputSink`, wrap for `outputFormat` etc
          sourceProvider.buffer().source(timeout = Timeout.NONE).use { streamSource ->
            dumpSourceWithProgressReporting(streamSource, outputSink, sourceSize)
          }
        }
      }
    }
  }

  private fun dumpSourceWithProgressReporting(streamSource: BufferedSource, outputSink: Sink, sourceSize: BigDecimal?) {
    ProgressSink(outputSink).use { progressSink ->
      if (sourceSize != null) {
        progressSink.addListener(StreamingProgressListener(sourceSize))
      }

      streamSource.readAll(progressSink)

      if (sourceSize != null) {
        println("[100%] $sourceSize / $sourceSize bytes")
      }
    }
  }

  private fun openOutputSinks(outputFormat: OutputFormat, dumpStandardOut: Boolean, outputFile: Path?): Sink {
    val outputSinks = listOfNotNull(
      System.out.sink().takeIf { dumpStandardOut },
      outputFile?.let { FileSystem.SYSTEM.sink(it, mustCreate = false) },
    )

    val outputSink = when {
      outputSinks.isEmpty() -> error("Must provide at least one output location")
      outputSinks.size == 1 -> outputSinks.single()
      else -> TeeSink(outputSinks, timeout = Timeout.NONE, closeAllOnClose = true)
    }

    return when (outputFormat) {
      OutputFormat.BIN -> outputSink
      OutputFormat.GZIP -> GzipSink(outputSink)
    }
  }

  private class StreamingProgressListener(sourceSize: BigDecimal) : ProgressSink.Listener {
    private val sourceSize = sourceSize.setScale(@Suppress("MagicNumber") 4)

    private var dumpedBytes: BigInteger = BigInteger.ZERO

    private var firstReported = false

    private val operationStopwatch: Stopwatch = Stopwatch.createStarted()
    private val reportStopwatch: Stopwatch = Stopwatch.createStarted()

    override fun onWrite(bytesWritten: Long) {
      dumpedBytes += bytesWritten.toBigInteger()

      if (!firstReported || reportStopwatch.elapsed() >= REPORT_CADENCE) {
        val progress = (dumpedBytes.toBigDecimal() / sourceSize * DECIMAL_PERCENT_TO_ONE_HUNDRED_FACTOR)
          .setScale(2)

        val millisElapsed = operationStopwatch.elapsed(TimeUnit.MILLISECONDS).toBigDecimal()
        val mibpsRate = if (millisElapsed > BigDecimal.ZERO) {
          dumpedBytes.toBigDecimal() / millisElapsed * BYTES_PER_MILLI_TO_MIBYTES_PER_SECOND_FACTOR
        } else {
          BigDecimal.ZERO
        }.setScale(2)

        println("[$progress%] $dumpedBytes / $sourceSize bytes ($mibpsRate MiB/second)")

        reportStopwatch.reset()
        reportStopwatch.start()

        firstReported = true
      }
    }
  }

  companion object {
    private val REPORT_CADENCE = Duration.ofMillis(250)
    private val DECIMAL_PERCENT_TO_ONE_HUNDRED_FACTOR = 100.toBigDecimal()
    private val BYTES_PER_MILLI_TO_MIBYTES_PER_SECOND_FACTOR =
      1000.toBigDecimal().setScale(4) / (1024 * 1024).toBigDecimal()
  }
}
