@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package net.navatwo.kaff4.rdf.io.literals

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.eclipse.rdf4j.model.Literal
import java.lang.Integer as JInt

@Singleton
internal class IntRdfConverter @Inject constructor() :
  PrimitiveRdfConverter<Int>(generateTypeLiterals<Int, JInt>()) {
  override fun parse(value: Literal): Int = value.intValue()
  override fun serialize(value: Int): Literal = valueFactory.createLiteral(value)
}
