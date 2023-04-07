package net.navatwo.kaff4.verify

import com.google.inject.Guice
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.vararg
import net.navatwo.guice.getInstance
import net.navatwo.kaff4.ReaderModule
import net.navatwo.kaff4.cli.PathArgType.Companion.InputPath

private const val DEFAULT_THREAD_COUNT = 8

@ExperimentalCli
class VerifySubcommand : Subcommand("verify", "Verify an image") {
  private val inputImages by argument(
    type = ArgType.InputPath,
    fullName = "input_file",
    description = "Input image to verify"
  ).vararg()

  private val verifyThreadCount by option(
    type = ArgType.Int,
    shortName = "n",
    fullName = "thread_count",
    description = "Number of threads to use for verification"
  ).default(DEFAULT_THREAD_COUNT)

  override fun execute() {
    val injector = Guice.createInjector(ReaderModule)

    val verifyAction = injector.getInstance<VerifyAction>()
    verifyAction.execute(inputImages, verifyThreadCount)
  }
}
