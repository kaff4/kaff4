package com.github.nava2.aff4.rdf.io.literals

import com.github.nava2.aff4.rdf.io.ConcreteRdfValueConverter
import com.github.nava2.guice.typeLiteral
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import java.time.ZonedDateTime
import javax.inject.Singleton

@Singleton
internal class ZonedDateTimeRdfConverter : ConcreteRdfValueConverter<ZonedDateTime>(typeLiteral<ZonedDateTime>()) {
  override fun parse(value: Value): ZonedDateTime? {
    val calendarValue = (value as? Literal)?.calendarValue() ?: return null
    return calendarValue.toGregorianCalendar().toZonedDateTime()
  }

  override fun serialize(value: ZonedDateTime): Value? {
    return valueFactory.createLiteral(value)
  }
}
