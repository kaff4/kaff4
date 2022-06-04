package com.github.nava2.aff4.rdf.io

import com.github.nava2.aff4.rdf.NamespacesProvider
import com.github.nava2.aff4.rdf.schema.RdfSchema
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class RdfModelSerializer @Inject internal constructor(
  private val rdfAnnotationTypeInfoLookup: RdfAnnotationTypeInfo.Lookup,
  private val valueConverterProvider: RdfValueConverterProvider,
  private val namespacesProvider: NamespacesProvider,
  private val valueFactory: ValueFactory,
) {
  fun <T : Any> serialize(value: T): Sequence<Statement> = sequence {
    val rdfInfo = rdfAnnotationTypeInfoLookup.get(value.javaClass.kotlin, namespacesProvider)

    val subject = rdfInfo.subjectProperty?.call(value)!!
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
        for (childValue in propertyValue.filterNotNull()) {
          yield(valueFactory.createStatement(subject, predicate, converter.serialize(info.elementType, childValue)))
        }
      } else if (propertyValue != null) {
        yield(valueFactory.createStatement(subject, predicate, converter.serialize(info.elementType, propertyValue)))
      }
    }
  }
}
