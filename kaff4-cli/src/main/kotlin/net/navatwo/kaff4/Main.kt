package net.navatwo.kaff4

import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli
import net.navatwo.kaff4.dump_image.DumpImageStreamSubcommand
import net.navatwo.kaff4.verify.VerifySubcommand

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) {
  val parser = ArgParser("kaff4-cli", strictSubcommandOptionsOrder = true)

  parser.subcommands(
    VerifySubcommand(),
    DumpImageStreamSubcommand(),
  )

  parser.parse(args)
}
