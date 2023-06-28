package net.navatwo.kaff4.model.rdf

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.navatwo.guice.typeLiteral
import net.navatwo.kaff4.rdf.io.ConcreteRdfValueConverter
import okio.Path
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value

@Singleton
internal class Aff4ImagePathRdfValueConverter @Inject constructor() :
  ConcreteRdfValueConverter<Path>(typeLiteral<Path>()) {
  override fun parse(value: Value): Path? {
    val path = (value as? Literal)?.label ?: return null
    return path.toPath()
  }

  override fun serialize(value: Path): Value {
    return valueFactory.createLiteral(value.toString())
  }
}
