package net.navatwo.kaff4.cli

import kotlinx.cli.ArgType
import kotlinx.cli.ParsingException
import net.navatwo.kaff4.model.rdf.Aff4Arn
import net.navatwo.kaff4.model.rdf.createArn
import org.eclipse.rdf4j.model.impl.SimpleValueFactory

internal object Aff4ArnArgType : ArgType<Aff4Arn>(hasParameter = true) {
  private val valueFactory = SimpleValueFactory.getInstance()

  override val description: kotlin.String
    get() = "{ Unique AFF4 identifier within a container in turtle form. }"

  override fun convert(value: kotlin.String, name: kotlin.String): Aff4Arn {
    return try {
      valueFactory.createArn(value)
    } catch (iae: IllegalArgumentException) {
      throw ParsingException("Option $name is expected to be IRI: $value").apply {
        addSuppressed(iae)
      }
    }
  }

  val Companion.Aff4Arn: Aff4ArnArgType get() = Aff4ArnArgType
}
