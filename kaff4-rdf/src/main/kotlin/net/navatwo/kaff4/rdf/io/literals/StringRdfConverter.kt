@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package net.navatwo.kaff4.rdf.io.literals

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.eclipse.rdf4j.model.Literal
import java.lang.String as JString

@Singleton
internal class StringRdfConverter @Inject constructor() :
  PrimitiveRdfConverter<String>(generateTypeLiterals<String, JString>()) {
  override fun parse(value: Literal): String = value.stringValue()
  override fun serialize(value: String): Literal = valueFactory.createLiteral(value)
}
