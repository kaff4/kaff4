package com.github.nava2.aff4.meta.rdf.io

import com.github.nava2.guice.typeLiteral
import com.google.inject.TypeLiteral
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Value
import java.math.BigInteger
import java.time.ZonedDateTime

abstract class RdfValueConverter<T>(val type: TypeLiteral<T>) {
  abstract fun convert(value: Value): T?

  object StringHandler : RdfValueConverter<String>(typeLiteral()) {
    override fun convert(value: Value): String? {
      return (value as? Literal)?.label
    }
  }

  object IntHandler : RdfValueConverter<Int>(typeLiteral()) {
    override fun convert(value: Value): Int? {
      return (value as? Literal)?.intValue()
    }
  }

  object BigIntegerHandler : RdfValueConverter<BigInteger>(typeLiteral()) {
    override fun convert(value: Value): BigInteger? {
      return (value as? Literal)?.integerValue()
    }
  }

  object DoubleHandler : RdfValueConverter<Double>(typeLiteral()) {
    override fun convert(value: Value): Double? {
      return (value as? Literal)?.doubleValue()
    }
  }

  object ZonedDateTimeConverter : RdfValueConverter<ZonedDateTime>(typeLiteral()) {
    override fun convert(value: Value): ZonedDateTime? {
      val calendarValue = (value as? Literal)?.calendarValue() ?: return null
      return calendarValue.toGregorianCalendar().toZonedDateTime()
    }
  }

  object IriHandler : RdfValueConverter<IRI>(typeLiteral()) {
    override fun convert(value: Value): IRI? {
      return value as? IRI
    }
  }

  object ResourceHandler : RdfValueConverter<Resource>(typeLiteral()) {
    override fun convert(value: Value): Resource? {
      return value as? Resource
    }
  }
}
