package com.github.nava2.aff4.rdf.io

import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.rdf.QueryableRdfConnection
import com.github.nava2.aff4.rdf.RdfValueConverter
import com.github.nava2.aff4.rdf.schema.RdfSchema
import com.github.nava2.aff4.yieldNotNull
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.ValueFactory
import kotlin.LazyThreadSafetyMode.NONE

class RdfModelSerializer @AssistedInject internal constructor(
  rdfAnnotationTypeInfoLookupFactory: RdfAnnotationTypeInfo.Lookup.Factory,
  private val valueConverterProvider: RdfValueConverterProvider,
  @Assisted toolDialect: ToolDialect,
) {
  private val rdfAnnotationTypeInfoLookup = rdfAnnotationTypeInfoLookupFactory.withDialect(toolDialect)

  fun <T : Any> serialize(connection: QueryableRdfConnection, value: T): Sequence<Statement> = sequence {
    val rdfInfo = rdfAnnotationTypeInfoLookup.get(value.javaClass.kotlin, connection.namespaces)

    val subject = rdfInfo.subjectProperty?.call(value)!!
    val valueFactory = connection.valueFactory

    val typeStatement = valueFactory.createStatement(
      subject,
      valueFactory.createIRI(RdfSchema.SCHEMA, "type"),
      rdfInfo.rdfType,
    )

    yield(typeStatement)

    for ((predicate, info) in rdfInfo.otherProperties.entries()) {
      val propertyValue = info.property.call(value)

      @Suppress("UNCHECKED_CAST")
      val converter by lazy(NONE) { valueConverterProvider.getConverter(info.elementType) as RdfValueConverter<Any> }

      if (propertyValue is Collection<*> && info.collectionType != null) {
        yieldAll(
          propertyValue.asSequence()
            .filterNotNull()
            .mapNotNull { converter.convertValue(valueFactory, info, it, subject, predicate) }
        )
      } else if (propertyValue != null) {
        yieldNotNull(converter.convertValue(valueFactory, info, propertyValue, subject, predicate))
      }
    }
  }

  private fun RdfValueConverter<Any>.convertValue(
    valueFactory: ValueFactory,
    info: PropertyInfo,
    value: Any,
    subject: Resource,
    predicate: IRI?,
  ): Statement? {
    val convertedValue = serialize(info.elementType, value)
    return convertedValue?.let { valueFactory.createStatement(subject, predicate, it) }
  }

  interface Factory {
    fun create(toolDialect: ToolDialect): RdfModelSerializer
  }
}
