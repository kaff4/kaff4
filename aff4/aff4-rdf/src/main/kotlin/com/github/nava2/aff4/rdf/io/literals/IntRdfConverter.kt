@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.github.nava2.aff4.rdf.io.literals

import org.eclipse.rdf4j.model.Literal
import javax.inject.Inject
import javax.inject.Singleton
import java.lang.Integer as JInt

@Singleton
internal class IntRdfConverter @Inject constructor()
  : PrimitiveRdfConverter<Int>(generateTypeLiterals<Int, JInt>()) {
  override fun parse(value: Literal): Int = value.intValue()
  override fun serialize(value: Int): Literal = valueFactory.createLiteral(value)
}
