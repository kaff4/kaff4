package net.navatwo.kaff4.dump_image

import com.google.common.base.Stopwatch
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.navatwo.kaff4.io.BufferedSource
import net.navatwo.kaff4.io.ProgressSink
import net.navatwo.kaff4.io.TeeSink
import net.navatwo.kaff4.io.buffer
import net.navatwo.kaff4.io.readAll
import net.navatwo.kaff4.model.Aff4ImageOpener
import net.navatwo.kaff4.model.Aff4StreamSourceProvider
import net.navatwo.kaff4.model.rdf.Aff4Arn
import okio.FileSystem
import okio.GzipSink
import okio.Path
import okio.Sink
import okio.Timeout
import okio.sink
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.Duration
import java.util.concurrent.TimeUnit

@Singleton
internal class DumpImageAction @Inject constructor(
  private val imageOpener: Aff4ImageOpener,
) {
  fun execute(
    imagePath: Path,
    streamIdentifier: Aff4Arn,
    outputFormat: OutputFormat,
    dumpStandardOut: Boolean,
    outputFile: Path?,
  ) {
    fun log(message: String) {
      if (!dumpStandardOut) {
        println(message)
      }
    }

    openOutputSinks(outputFormat, dumpStandardOut, outputFile).use { outputSink ->
      imageOpener.open(FileSystem.SYSTEM, imagePath) { container ->
        log("Opened [$imagePath]")

        val streamOpener = container.streamOpener
        streamOpener.openStream(streamIdentifier).use { sourceProvider ->
          val sourceSize = (sourceProvider as? Aff4StreamSourceProvider)?.size?.toBigDecimal()

          sourceProvider.buffer().source(timeout = Timeout.NONE).use { streamSource ->
            dumpSourceWithProgressReporting(streamSource, outputSink, sourceSize, ::log)
          }
        }
      }
    }

    if (outputFile != null) {
      log("Dumped image to $outputFile")
    }
  }

  private fun dumpSourceWithProgressReporting(
    streamSource: BufferedSource,
    outputSink: Sink,
    sourceSize: BigDecimal?,
    log: (String) -> Unit,
  ) {
    ProgressSink(outputSink).use { progressSink ->
      if (sourceSize != null) {
        progressSink.addListener(StreamingProgressListener(sourceSize, log))
      }

      streamSource.readAll(progressSink)

      if (sourceSize != null) {
        log("[100%] $sourceSize / $sourceSize bytes")
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

  enum class OutputFormat {
    BIN,
    GZIP,
  }

  private class StreamingProgressListener(
    sourceSize: BigDecimal,
    private val log: (message: String) -> Unit,
  ) : ProgressSink.Listener {
    private val sourceSize = sourceSize.setScale(@Suppress("MagicNumber") 4)

    private var dumpedBytes: BigInteger = BigInteger.ZERO

    private var firstReported = false

    private val operationStopwatch: Stopwatch = Stopwatch.createStarted()
    private val reportStopwatch: Stopwatch = Stopwatch.createStarted()

    override fun onWrite(bytesWritten: Long) {
      dumpedBytes += bytesWritten.toBigInteger()

      if (!firstReported || reportStopwatch.elapsed() >= REPORT_CADENCE) {
        val progress = computeProgress()

        val millisElapsed = operationStopwatch.elapsed(TimeUnit.MILLISECONDS).toBigDecimal()
        val mibpsRate = if (millisElapsed > BigDecimal.ZERO) {
          dumpedBytes.toBigDecimal().setScale(2) / millisElapsed * BYTES_PER_MILLI_TO_MIBIYTES_PER_SECOND_FACTOR
        } else {
          BigDecimal.ZERO
        }.setScale(@Suppress("MagicNumber") 3, RoundingMode.UP)

        log.invoke(
          "[$progress%] $dumpedBytes / ${sourceSize.setScale(0)} bytes ($mibpsRate MiB/second)"
        )

        reportStopwatch.reset()
        reportStopwatch.start()

        firstReported = true
      }
    }

    private fun computeProgress(): BigDecimal {
      val ratioComplete = dumpedBytes.toBigDecimal().setScale(@Suppress("MagicNumber") 4) / sourceSize
      val progress = ratioComplete * DECIMAL_PERCENT_TO_ONE_HUNDRED_FACTOR
      return progress.coerceAtMost(MAXIMUM_PROGRESS_REPORTED).setScale(2)
    }

    companion object {
      private val REPORT_CADENCE = Duration.ofMillis(250)
      private val MAXIMUM_PROGRESS_REPORTED = "99.9999".toBigDecimal()
      private val DECIMAL_PERCENT_TO_ONE_HUNDRED_FACTOR = 100.toBigDecimal()
      private val BYTES_PER_MILLI_TO_MIBIYTES_PER_SECOND_FACTOR =
        1000.toBigDecimal().setScale(4) / (1024 * 1024).toBigDecimal()
    }
  }
}
