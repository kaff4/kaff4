@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.github.nava2.aff4.rdf.io.literals

import org.eclipse.rdf4j.model.Literal
import javax.inject.Singleton
import java.lang.Float as JFloat

@Singleton
internal class FloatRdfConverter : PrimitiveRdfConverter<Float>(generateTypeLiterals<Float, JFloat>()) {
  override fun parse(value: Literal): Float = value.floatValue()
  override fun serialize(value: Float): Literal = valueFactory.createLiteral(value)
}
