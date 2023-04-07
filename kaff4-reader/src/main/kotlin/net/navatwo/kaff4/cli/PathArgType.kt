package net.navatwo.kaff4.cli

import kotlinx.cli.ArgType
import kotlinx.cli.ParsingException
import okio.Path
import okio.Path.Companion.toOkioPath
import java.nio.file.Paths
import kotlin.io.path.exists

internal class PathArgType private constructor(private val requireExistingPath: kotlin.Boolean) :
  ArgType<Path>(hasParameter = true) {
  override val description: kotlin.String
    get() = "{ Value is a path to a file }"

  override fun convert(value: kotlin.String, name: kotlin.String): okio.Path {
    val path = Paths.get(value)

    if (requireExistingPath && !path.exists()) {
      throw ParsingException("Option $name is expected to be an existing path: $value")
    }

    return path.toOkioPath()
  }

  companion object {
    val ArgType.Companion.InputPath: PathArgType
      get() = PathArgType(requireExistingPath = true)

    val ArgType.Companion.OutputPath: PathArgType
      get() = PathArgType(requireExistingPath = false)
  }
}
