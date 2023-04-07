package net.navatwo.kaff4.dump_image

import com.google.inject.Guice
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import net.navatwo.guice.getInstance
import net.navatwo.kaff4.ReaderModule
import net.navatwo.kaff4.cli.Aff4ArnArgType.Aff4Arn
import net.navatwo.kaff4.cli.PathArgType.Companion.InputPath
import net.navatwo.kaff4.cli.PathArgType.Companion.OutputPath
import net.navatwo.kaff4.dump_image.DumpImageAction.OutputFormat

@ExperimentalCli
internal class DumpImageStreamSubcommand : Subcommand("dump-stream", "Dump an image stream") {
  private val inputPath by argument(
    type = ArgType.InputPath,
    fullName = "input_container",
    description = "Input container to read"
  )

  private val streamIdentifier by argument(
    type = ArgType.Aff4Arn,
    fullName = "stream-name",
    description = "Stream within {input_container} to dump",
  )

  private val dumpStandardOut by option(
    type = ArgType.Boolean,
    fullName = "stdout",
    description = "Dump the output to the standard output, useful for piping to other applications",
  ).default(false)

  private val outputFile by option(
    type = ArgType.OutputPath,
    fullName = "output_file",
    description = "Output file to dump to",
  )

  private val outputFormat by option(
    type = ArgType.Choice<OutputFormat>(),
    fullName = "format",
    description = "Output format to use",
  ).default(OutputFormat.BIN)

  override fun execute() {
    val injector = Guice.createInjector(ReaderModule)

    val verifyAction = injector.getInstance<DumpImageAction>()
    verifyAction.execute(
      imagePath = inputPath,
      streamIdentifier = streamIdentifier,
      outputFormat = outputFormat,
      dumpStandardOut = dumpStandardOut,
      outputFile = outputFile
    )
  }
}
