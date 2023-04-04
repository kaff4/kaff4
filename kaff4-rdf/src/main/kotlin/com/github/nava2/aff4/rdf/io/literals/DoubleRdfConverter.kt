@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.github.nava2.aff4.rdf.io.literals

import org.eclipse.rdf4j.model.Literal
import javax.inject.Inject
import javax.inject.Singleton
import java.lang.Double as JDouble

@Singleton
internal class DoubleRdfConverter @Inject constructor() :
  PrimitiveRdfConverter<Double>(generateTypeLiterals<Double, JDouble>()) {
  override fun parse(value: Literal): Double = value.doubleValue()
  override fun serialize(value: Double): Literal = valueFactory.createLiteral(value)
}
