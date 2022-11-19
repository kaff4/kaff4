package com.github.nava2.aff4.rdf.io.literals

import com.github.nava2.aff4.rdf.RdfValueConverter
import com.github.nava2.guice.typeLiteral
import org.eclipse.rdf4j.model.Value
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EnumRdfConverter @Inject constructor(
  private val stringRdfConverter: StringRdfConverter,
) : RdfValueConverter<Enum<*>>(typeLiteral<Enum<*>>()) {
  override fun matches(clazz: Class<*>): Boolean = clazz.isEnum

  override fun parse(clazz: Class<*>, value: Value): Enum<*>? {
    val asString = stringRdfConverter.parse(String::class.java, value)
    return clazz.enumConstants.asSequence()
      .map { it as Enum<*> }
      .find { it.name == asString }
  }

  override fun serialize(clazz: Class<*>, value: Enum<*>): Value? {
    return stringRdfConverter.serialize(String::class.java, value.name)
  }
}
