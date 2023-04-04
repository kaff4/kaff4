package net.navatwo.kaff4.rdf.io

import misk.scope.ActionScoped
import net.navatwo.kaff4.rdf.QueryableRdfConnection
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

internal class RealRdfModelParser @Inject constructor(
  private val rdfAnnotationTypeInfoLookupProvider: ActionScoped<RdfAnnotationTypeInfo.Lookup>,
  private val valueConverterProvider: RdfValueConverterProvider,
) : RdfModelParser {
  override fun <T : Any> parse(
    rdfConnection: QueryableRdfConnection,
    type: KClass<T>,
    subject: Resource,
    statements: Collection<Statement>,
  ): T {
    val rdfAnnotationTypeInfo = rdfAnnotationTypeInfoLookupProvider.get().get(type, rdfConnection.namespaces)

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

  val aggregateValues = statementsAndParameters.asSequence()
    .flatMap { (statement, parameters) ->
      parameters.asSequence().map { parameter ->
        val obj = statement.`object`
        val handler = valueConverterProvider.getConverter(parameter.elementType)
        parameter to handler.parse(parameter.elementType, obj)
      }
    }
    .groupBy(
      keySelector = { (propertyInfo, _) -> propertyInfo },
      valueTransform = { (_, converted) -> converted },
    )

  for ((parameter, values) in aggregateValues) {
    parameterMap[parameter.parameter] = when (parameter.collectionType) {
      null -> values.singleOrNull() ?: error("Found multiple values for ${parameter.parameter}: $values")
      List::class.java -> values
      Set::class.java -> values.toSet()
      else -> error("error: unsupported type: ${parameter.collectionType}")
    }
  }
}