@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package net.navatwo.kaff4.rdf.io.literals

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.eclipse.rdf4j.model.Literal
import java.lang.Float as JFloat

@Singleton
internal class FloatRdfConverter @Inject constructor() :
  PrimitiveRdfConverter<Float>(generateTypeLiterals<Float, JFloat>()) {
  override fun parse(value: Literal): Float = value.floatValue()
  override fun serialize(value: Float): Literal = valueFactory.createLiteral(value)
}
