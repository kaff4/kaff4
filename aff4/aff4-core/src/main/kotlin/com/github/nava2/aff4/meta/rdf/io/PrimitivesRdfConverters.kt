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

private inline fun <reified KOTLIN, reified JAVA> primitiveConverter(
  noinline convertBlock: (value: Literal) -> KOTLIN?,
): RdfValueConverter<KOTLIN> {
  val kotlinType = typeLiteral<KOTLIN>()
  val javaType = typeLiteral<JAVA>()
  val primitiveType = ClassUtils.wrapperToPrimitive(JAVA::class.java)?.let { TypeLiteral.get(it) }
  val types = setOfNotNull(kotlinType, javaType, primitiveType)

  return literalConverter(types.toTypedArray(), convertBlock)
}

private inline fun <reified KOTLIN> literalConverter(
  types: Array<TypeLiteral<*>> = arrayOf(typeLiteral<KOTLIN>()),
  noinline convertBlock: (value: Literal) -> KOTLIN?,
): RdfValueConverter<KOTLIN> {
  return object : RdfValueConverter<KOTLIN>(types = types) {
    override fun convert(value: Value): KOTLIN? = (value as Literal?)?.let { convertBlock(it) }

    override fun toString(): String {
      return "${RdfValueConverter::class.simpleName}<${KOTLIN::class.simpleName}>"
    }
  }
}
