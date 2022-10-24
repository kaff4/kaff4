@file:OptIn(ExperimentalCli::class)

package com.github.nava2.aff4

import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli

fun main(args: Array<String>) {
  val parser = ArgParser("kaff4-cli", strictSubcommandOptionsOrder = true)

  val verify = Verify()
  parser.subcommands(verify)

  parser.parse(args)
}
