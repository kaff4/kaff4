package com.github.nava2.aff4

import com.github.nava2.aff4.verify.VerifySubcommand
import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) {
  val parser = ArgParser("kaff4-cli", strictSubcommandOptionsOrder = true)

  val verify = VerifySubcommand()
  parser.subcommands(verify)

  parser.parse(args)
}
