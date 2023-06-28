@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package net.navatwo.kaff4.rdf.io.literals

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.eclipse.rdf4j.model.Literal
import java.lang.Long as JLong

@Singleton
internal class LongRdfConverter @Inject constructor() :
  PrimitiveRdfConverter<Long>(generateTypeLiterals<Long, JLong>()) {
  override fun parse(value: Literal): Long = value.longValue()
  override fun serialize(value: Long): Literal = valueFactory.createLiteral(value)
}
