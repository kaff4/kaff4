@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.github.nava2.aff4.rdf.io.literals

import org.eclipse.rdf4j.model.Literal
import javax.inject.Singleton
import java.lang.String as JString

@Singleton
internal class StringRdfConverter : PrimitiveRdfConverter<String>(generateTypeLiterals<String, JString>()) {
  override fun parse(value: Literal): String = value.stringValue()
  override fun serialize(value: String): Literal = valueFactory.createLiteral(value)
}
