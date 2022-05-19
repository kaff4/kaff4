package com.github.nava2.aff4.meta.rdf.io

import com.github.nava2.aff4.meta.rdf.NamespacesProvider
import org.apache.commons.lang3.ClassUtils
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

class RdfModelParser @Inject internal constructor(
  private val namespacesProvider: NamespacesProvider,
  private val valueConverterProvider: RdfValueConverterProvider,
) {
  private val constructionInfoMap: MutableMap<KClass<*>, ConstructionInfo<*>> = mutableMapOf()

  fun <T : Any> parse(type: KClass<T>, subject: Resource, statements: Collection<Statement>): T {
    val constructionInfo = getOrCreateConstructionInfo(type)

    return parse(constructionInfo, subject, statements)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T : Any> getOrCreateConstructionInfo(type: KClass<T>) = constructionInfoMap.getOrPut(type) {
    val constructor = findRdfConstructor(type)

    val memberProperties = type.memberProperties.associateBy { it.name }
    val memoizedPropertyMaps = mutableMapOf<KClass<*>, Map<String, KProperty<*>>>(type to memberProperties)

    val subjectParams = mutableSetOf<KParameter>()

    val otherParams = mutableMapOf<KParameter, ParameterInfo>()
    for (parameter in constructor.parameters) {
      if (isAnnotationPresent<RdfSubject>(memoizedPropertyMaps, type, parameter)) {
        subjectParams.add(parameter)
        continue
      }

      val predicate = namespacesProvider.iriFromTurtle(
        findAnnotation<RdfValue>(memoizedPropertyMaps, type, parameter)?.turtleRdfIri
          ?: "aff4:${parameter.name}"
      )

      otherParams[parameter] = ParameterInfo(parameter, predicate)
    }

    val rdfType = (type.findAnnotation<RdfModel>() ?: constructor.findAnnotation())!!.rdfType
    ConstructionInfo(type, constructor, namespacesProvider.iriFromTurtle(rdfType), subjectParams, otherParams)
  } as ConstructionInfo<T>

  private fun <T : Any> parse(
    constructionInfo: ConstructionInfo<T>,
    subject: Resource,
    statements: Collection<Statement>,
  ): T {
    val parameterMap = LinkedHashMap<KParameter, Any?>(constructionInfo.requiredParameters.size)
    for (subjectParam in constructionInfo.subjectParams) {
      parameterMap[subjectParam] = subject
    }

    buildNonSubjectParamMap(parameterMap, constructionInfo, statements, valueConverterProvider)

    return constructionInfo.constructor.callBy(parameterMap)
  }
}

private fun <T : Any> buildNonSubjectParamMap(
  parameterMap: MutableMap<KParameter, Any?>,
  constructionInfo: ConstructionInfo<T>,
  statements: Collection<Statement>,
  valueConverterProvider: RdfValueConverterProvider
) {
  val aggregateValues = mutableMapOf<ParameterInfo, MutableList<Any?>>()
  for ((statement, parameters) in statements.associateWith { constructionInfo.parametersByPredicate[it.predicate] }) {
    if (parameters == null) continue

    for (parameter in parameters) {
      val obj = statement.`object`
      val handler = valueConverterProvider.getConverter(parameter.elementType)
      val converted = handler.convert(parameter.elementType, obj)

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
  val elementType: Class<*>

  init {
    val javaType = parameter.type.javaType

    val elementType: Type
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
      collectionType = null
      elementType = javaType
    }

    elementType as Class<*>
    this.elementType = if (
      ClassUtils.isPrimitiveOrWrapper(elementType) &&
      ClassUtils.isPrimitiveWrapper(elementType)
    ) {
      ClassUtils.wrapperToPrimitive(elementType)
    } else {
      elementType
    }
  }
}

private fun <A : Annotation> walkHierarchyForPropertyAnnotation(
  memoizedPropertyMaps: MutableMap<KClass<*>, Map<String, KProperty<*>>>,
  annKlass: KClass<A>,
  klass: KClass<*>,
  leafProperty: KProperty<*>,
): A? {
  // Open properties mean that it is either explicitly open (womp - wasted cycles) or
  // it is extending another class
  if (!leafProperty.isOpen) return null

  val queue = ArrayDeque(klass.supertypes)
  val visited = mutableSetOf<KType>()

  val propertyName = leafProperty.name

  var fromSuperKlass: A? = null

  while (!queue.isEmpty() && fromSuperKlass == null) {
    // avoid revisiting
    val superType = queue.removeFirst().takeIf { visited.add(it) }

    val superKlass = superType?.classifier as? KClass<*>
    val superMemberProperty = superKlass?.let { getMemberByName(memoizedPropertyMaps, it, propertyName) }
      ?: continue

    @Suppress("UNCHECKED_CAST")
    fromSuperKlass = superMemberProperty.annotations.firstOrNull { annKlass.isInstance(it) } as? A

    if (superMemberProperty.isOpen) {
      queue.addAll(superKlass.supertypes)
    }
  }

  return fromSuperKlass
}

private inline fun <reified A : Annotation> findAnnotation(
  memoizedPropertyMaps: MutableMap<KClass<*>, Map<String, KProperty<*>>>,
  klass: KClass<*>,
  parameter: KParameter,
): A? {
  val fromParameter = parameter.findAnnotation<A>()
  if (fromParameter != null) return fromParameter

  return findAnnotationForPropertyNamed(memoizedPropertyMaps, A::class, klass, parameter.name!!)
}

private fun <A : Annotation> findAnnotationForPropertyNamed(
  memoizedPropertyMaps: MutableMap<KClass<*>, Map<String, KProperty<*>>>,
  annKlass: KClass<A>,
  klass: KClass<*>,
  propertyName: String,
): A? {
  val memberProperty = getMemberByName(memoizedPropertyMaps, klass, propertyName)

  @Suppress("UNCHECKED_CAST")
  val fromMemberProperty = memberProperty?.annotations?.firstOrNull { annKlass.isInstance(it) } as? A
  if (fromMemberProperty != null) return fromMemberProperty

  return memberProperty?.let {
    walkHierarchyForPropertyAnnotation(
      memoizedPropertyMaps = memoizedPropertyMaps,
      annKlass = annKlass,
      klass = klass,
      leafProperty = memberProperty,
    )
  }
}

private fun getMemberByName(
  memoizedPropertyMaps: MutableMap<KClass<*>, Map<String, KProperty<*>>>,
  klass: KClass<*>,
  propertyName: String,
): KProperty<*>? {
  val memberProperties = memoizedPropertyMaps.getOrPut(klass) {
    klass.memberProperties.associateBy { it.name }
  }
  return memberProperties[propertyName]
}

private inline fun <reified A : Annotation> isAnnotationPresent(
  memoizedPropertyMaps: MutableMap<KClass<*>, Map<String, KProperty<*>>>,
  klass: KClass<*>,
  parameter: KParameter,
): Boolean = findAnnotation<A>(memoizedPropertyMaps, klass, parameter) != null
