@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package net.navatwo.kaff4.rdf.io.literals

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.eclipse.rdf4j.model.Literal
import java.lang.Double as JDouble

@Singleton
internal class DoubleRdfConverter @Inject constructor() :
  PrimitiveRdfConverter<Double>(generateTypeLiterals<Double, JDouble>()) {
  override fun parse(value: Literal): Double = value.doubleValue()
  override fun serialize(value: Double): Literal = valueFactory.createLiteral(value)
}
