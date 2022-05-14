@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.github.nava2.aff4.meta.rdf.io

import com.github.nava2.guice.typeLiteral
import com.google.inject.TypeLiteral
import org.apache.commons.lang3.ClassUtils
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import kotlin.Double
import kotlin.Float
import kotlin.Long
import kotlin.String
import java.lang.Double as JDouble
import java.lang.Float as JFloat
import java.lang.Integer as JInteger
import java.lang.Long as JLong
import java.lang.String as JString

internal val StringRdfConverter = primitiveConverter<String, JString> { it.label }

internal val IntRdfConverter = primitiveConverter<Int, JInteger> { it.intValue() }

internal val LongRdfConverter = primitiveConverter<Long, JLong> { it.longValue() }

internal val DoubleRdfConverter = primitiveConverter<Double, JDouble> { it.doubleValue() }

internal val FloatRdfConverter = primitiveConverter<Float, JFloat> { it.floatValue() }

internal val BigIntegerHandler = literalConverter { it.integerValue() }

internal val ZonedDateTimeConverter = literalConverter { value ->
  val calendarValue = (value as? Literal)?.calendarValue() ?: return@literalConverter null
  calendarValue.toGregorianCalendar().toZonedDateTime()
}

object EnumConverter : RdfValueConverter<Enum<*>>(typeLiteral<Enum<*>>()) {
  override fun matches(clazz: Class<*>): Boolean = clazz.isEnum

  override fun convert(clazz: Class<*>, value: Value): Enum<*>? {
    val asString = StringRdfConverter.convert(String::class.java, value)
    return clazz.enumConstants.asSequence()
      .map { it as Enum<*> }
      .find { it.name == asString }
  }
}

private inline fun <reified KOTLIN, reified JAVA> primitiveConverter(
  noinline convertBlock: (value: Literal) -> KOTLIN?,
): RdfValueConverter<KOTLIN> {
  val kotlinType = typeLiteral<KOTLIN>()
  val javaType = typeLiteral<JAVA>()
  val primitiveType = ClassUtils.wrapperToPrimitive(JAVA::class.java)?.let { TypeLiteral.get(it) }
  val types = setOfNotNull(kotlinType, javaType, primitiveType)

  return literalConverter(types, convertBlock)
}

private inline fun <reified KOTLIN> literalConverter(
  types: Set<TypeLiteral<*>> = setOf(typeLiteral<KOTLIN>()),
  noinline convertBlock: (value: Literal) -> KOTLIN?,
): RdfValueConverter<KOTLIN> {
  return ConcreteLiteralRdfValueConverter(types, convertBlock)
}

private data class ConcreteLiteralRdfValueConverter<KOTLIN>(
  private val matchingTypes: Set<TypeLiteral<*>>,
  private val convertBlock: (value: Literal) -> KOTLIN?
) : ConcreteRdfValueConverter<KOTLIN>(types = matchingTypes.toTypedArray()) {
  override fun convert(value: Value): KOTLIN? = (value as Literal?)?.let { convertBlock(it) }
}
