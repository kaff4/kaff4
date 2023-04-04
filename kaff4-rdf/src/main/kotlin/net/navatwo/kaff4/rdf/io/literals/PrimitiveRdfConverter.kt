@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package net.navatwo.kaff4.rdf.io.literals

import com.google.inject.TypeLiteral
import net.navatwo.guice.typeLiteral
import net.navatwo.kaff4.rdf.io.ConcreteRdfValueConverter
import org.apache.commons.lang3.ClassUtils
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value

abstract class PrimitiveRdfConverter<KOTLIN> internal constructor(
  matchingTypes: Set<TypeLiteral<*>>,
) : ConcreteRdfValueConverter<KOTLIN>(matchingTypes) {

  override fun parse(value: Value): KOTLIN? = (value as? Literal)?.let { parse(it) }

  abstract override fun serialize(value: KOTLIN): Literal

  protected abstract fun parse(value: Literal): KOTLIN
}

internal inline fun <reified JAVA, reified KOTLIN> generateTypeLiterals(): Set<TypeLiteral<out Any?>> {
  val kotlinType = typeLiteral<KOTLIN>()
  val javaType = typeLiteral<JAVA>()
  val primitiveType = ClassUtils.wrapperToPrimitive(JAVA::class.java)?.let { TypeLiteral.get(it) }
  return setOfNotNull(kotlinType, javaType, primitiveType)
}
