package com.github.nava2.aff4.meta.rdf.io

import com.github.nava2.aff4.meta.rdf.NamespacesProvider
import com.google.inject.TypeLiteral
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import javax.inject.Inject
import javax.inject.Provider
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

class RdfModelParser @Inject constructor(
  private val namespacesProvider: NamespacesProvider,
  private val handlersProvider: Provider<Map<TypeLiteral<*>, RdfValueConverter<*>>>,
) {
  private val constructionInfoMap: MutableMap<KClass<*>, ConstructionInfo<*>> = mutableMapOf()

  fun <T : Any> parse(type: KClass<T>, subject: Resource, statements: List<Statement>): T {
    val constructionInfo = constructionInfoMap.getOrPut(type) {
      val constructor = findRdfConstructor(type)

      val memberProperties = type.memberProperties.associateBy { it.name }

      val subjectParams = mutableSetOf<KParameter>()

      val otherParams = mutableMapOf<KParameter, ParameterInfo>()
      for (parameter in constructor.parameters) {
        if (
          parameter.hasAnnotation<RdfSubject>() ||
          memberProperties[parameter.name]?.hasAnnotation<RdfSubject>() == true
        ) {
          subjectParams.add(parameter)
          continue
        }

        val predicate = namespacesProvider.iriFromTurtle(
          parameter.findAnnotation<RdfValue>()?.turtleRdfIri
            ?: memberProperties[parameter.name]?.findAnnotation<RdfValue>()?.turtleRdfIri
            ?: "aff4:${parameter.name}"
        )

        otherParams[parameter] = ParameterInfo(parameter, predicate)
      }

      val rdfType = (type.findAnnotation<RdfModel>() ?: constructor.findAnnotation())!!.rdfType
      ConstructionInfo(type, constructor, namespacesProvider.iriFromTurtle(rdfType), subjectParams, otherParams)
    } as ConstructionInfo<T>

    return parse(constructionInfo, subject, statements)
  }

  private fun <T : Any> parse(
    constructionInfo: ConstructionInfo<T>,
    subject: Resource,
    statements: List<Statement>,
  ): T {
    val handlersMap = handlersProvider.get()

    val parameterMap = LinkedHashMap<KParameter, Any?>(constructionInfo.requiredParameters.size)
    for (subjectParam in constructionInfo.subjectParams) {
      parameterMap[subjectParam] = subject
    }

    buildNonSubjectParamMap(parameterMap, constructionInfo, statements, handlersMap)

    return constructionInfo.constructor.callBy(parameterMap)
  }

  private fun <T : Any> buildNonSubjectParamMap(
    parameterMap: MutableMap<KParameter, Any?>,
    constructionInfo: ConstructionInfo<T>,
    statements: List<Statement>,
    handlersMap: Map<TypeLiteral<*>, RdfValueConverter<*>>
  ) {
    val aggregateValues = mutableMapOf<ParameterInfo, MutableList<Any?>>()
    for ((statement, parameters) in statements.associateWith { constructionInfo.parametersByPredicate[it.predicate] }) {
      if (parameters == null) continue

      for (parameter in parameters) {
        val obj = statement.`object`
        val handler = handlersMap.getValue(TypeLiteral.get(parameter.elementType))
        val converted = handler.convert(obj)

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

  private fun <T : Any> findRdfConstructor(type: KClass<T>): KFunction<T> {
    val constructor = type.constructors.firstOrNull { ctor -> ctor.hasAnnotation<RdfModel>() }
      ?: type.primaryConstructor
      ?: error("Class $type is does not have a constructor supporting @${RdfModel::class} annotations")
    constructor.isAccessible = true
    return constructor
  }

  private data class ConstructionInfo<T : Any>(
    val klass: KClass<T>,
    val constructor: KFunction<T>,
    val rdfType: IRI,
    val subjectParams: Set<KParameter>,
    val otherParams: Map<KParameter, ParameterInfo>,
  ) {
    val requiredParameters = constructor.parameters.filter { !it.isOptional }.toSet()

    val parametersByPredicate = otherParams.entries.groupBy({ it.value.predicate }) { it.value }
  }

  private data class ParameterInfo(
    val parameter: KParameter,
    val predicate: IRI,
  ) {
    val collectionType: Class<*>?
    val elementType: Type

    init {
      val javaType = parameter.type.javaType

      if (javaType is ParameterizedType) {
        if (javaType.rawType == List::class.java || javaType.rawType == Set::class.java) {
          collectionType = javaType.rawType as Class<*>
          elementType = when (val argType = javaType.actualTypeArguments.single()) {
            is WildcardType -> argType.upperBounds.single()
            else -> argType
          }
        } else {
          collectionType = null
          elementType = javaType
        }
      } else {
        elementType = javaType
        collectionType = null
      }
    }
  }
}
