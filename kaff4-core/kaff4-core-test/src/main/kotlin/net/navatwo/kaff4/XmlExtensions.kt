package net.navatwo.kaff4

import java.time.ZonedDateTime
import javax.xml.datatype.DatatypeFactory

private val dataTypeFactory = DatatypeFactory.newDefaultInstance()

fun parseZonedDateTime(parse: String): ZonedDateTime {
  return dataTypeFactory.newXMLGregorianCalendar(parse)
    .toGregorianCalendar()
    .toZonedDateTime()
}
