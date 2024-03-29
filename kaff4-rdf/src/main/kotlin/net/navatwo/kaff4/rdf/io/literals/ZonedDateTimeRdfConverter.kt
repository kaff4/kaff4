package net.navatwo.kaff4.rdf.io.literals

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.navatwo.guice.typeLiteral
import net.navatwo.kaff4.rdf.io.ConcreteRdfValueConverter
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import java.time.ZonedDateTime

@Singleton
internal class ZonedDateTimeRdfConverter @Inject constructor() :
  ConcreteRdfValueConverter<ZonedDateTime>(typeLiteral<ZonedDateTime>()) {
  override fun parse(value: Value): ZonedDateTime? {
    val calendarValue = (value as? Literal)?.calendarValue() ?: return null
    return calendarValue.toGregorianCalendar().toZonedDateTime()
  }

  override fun serialize(value: ZonedDateTime): Value? {
    return valueFactory.createLiteral(value)
  }
}
