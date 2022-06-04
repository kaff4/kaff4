package com.github.nava2.aff4.rdf.io

import com.github.nava2.aff4.rdf.NamespacesProvider
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

class RdfModelParser @Inject internal constructor(
  private val rdfAnnotationTypeInfoLookup: RdfAnnotationTypeInfo.Lookup,
  private val valueConverterProvider: RdfValueConverterProvider,
  private val namespacesProvider: NamespacesProvider,
) {
  fun <T : Any> parse(type: KClass<T>, subject: Resource, statements: Collection<Statement>): T {
    val constructionInfo = rdfAnnotationTypeInfoLookup.get(type, namespacesProvider)
    return parse(constructionInfo, subject, statements)
  }

  private fun <T : Any> parse(
    rdfAnnotationTypeInfo: RdfAnnotationTypeInfo<T>,
    subject: Resource,
    statements: Collection<Statement>,
  ): T {
    val parameterMap = LinkedHashMap<KParameter, Any?>(rdfAnnotationTypeInfo.requiredParameters.size)
    for (subjectInfo in rdfAnnotationTypeInfo.subjectProperties) {
      parameterMap[subjectInfo.parameter] = subject
    }

    for (parameter in rdfAnnotationTypeInfo.otherProperties.values().map { it.parameter }) {
      if (parameter.type.isMarkedNullable) {
        parameterMap[parameter] = null
      }
    }

    buildNonSubjectParamMap(parameterMap, rdfAnnotationTypeInfo, statements, valueConverterProvider)

    return rdfAnnotationTypeInfo.constructor.callBy(parameterMap)
  }
}

private fun <T : Any> buildNonSubjectParamMap(
  parameterMap: MutableMap<KParameter, Any?>,
  rdfAnnotationTypeInfo: RdfAnnotationTypeInfo<T>,
  statements: Collection<Statement>,
  valueConverterProvider: RdfValueConverterProvider,
) {
  val statementsAndParameters = statements.associateWith {
    rdfAnnotationTypeInfo.otherProperties[it.predicate]
  }

  val aggregateValues = mutableMapOf<PropertyInfo, MutableList<Any?>>()
  for ((statement, parameters) in statementsAndParameters) {
    if (parameters == null) continue

    for (parameter in parameters) {
      val obj = statement.`object`
      val handler = valueConverterProvider.getConverter(parameter.elementType)
      val converted = handler.parse(parameter.elementType, obj)

      val values = aggregateValues.getOrPut(parameter) { mutableListOf() }
      values.add(converted)
    }
  }

  for ((parameter, values) in aggregateValues) {
    parameterMap[parameter.parameter] = when (parameter.collectionType) {
      null -> values.single()
      List::class.java -> values
      Set::class.java -> values.toSet()
      else -> error("error: unsupported type: ${parameter.collectionType}")
    }
  }
}
