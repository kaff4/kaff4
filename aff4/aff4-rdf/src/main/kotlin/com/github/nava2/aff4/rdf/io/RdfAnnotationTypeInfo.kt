package com.github.nava2.aff4.rdf.io

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.nava2.aff4.model.rdf.TurtleIri.Companion.toTurtleIri
import com.github.nava2.aff4.model.rdf.annotations.RdfModel
import com.github.nava2.aff4.model.rdf.annotations.RdfSubject
import com.github.nava2.aff4.model.rdf.annotations.RdfValue
import com.github.nava2.aff4.rdf.NamespacesProvider
import com.google.common.collect.ImmutableMultimap
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import javax.inject.Inject
import javax.inject.Singleton
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

private const val CACHE_SIZE = 20L

internal data class RdfAnnotationTypeInfo<T : Any>(
  val klass: KClass<T>,
  val rdfType: IRI,
  val subjectProperties: Set<PropertyInfo>,
  val otherProperties: ImmutableMultimap<IRI, PropertyInfo>,
) {
  val constructor = findRdfConstructor(klass)

  @Suppress("UNCHECKED_CAST")
  val subjectProperty: KProperty<Resource>? = subjectProperties.firstOrNull()?.property as? KProperty<Resource>

  val requiredParameters = constructor.parameters.filter { !it.isOptional }.toSet()

  @Singleton
  class Lookup @Inject constructor() {
    private val cache = Caffeine.newBuilder()
      .maximumSize(CACHE_SIZE)
      .build<KClass<*>, RdfAnnotationTypeInfo<*>>()

    fun <T : Any> get(type: KClass<T>, namespacesProvider: NamespacesProvider): RdfAnnotationTypeInfo<T> {
      val fromCache = cache.get(type) { key ->
        @Suppress("UNCHECKED_CAST")
        computeInfo(key as KClass<T>, namespacesProvider)
      }

      @Suppress("UNCHECKED_CAST")
      return fromCache as RdfAnnotationTypeInfo<T>
    }

    private fun <T : Any> computeInfo(
      type: KClass<T>,
      namespacesProvider: NamespacesProvider,
    ): RdfAnnotationTypeInfo<T> {
      val constructor = findRdfConstructor(type)

      val memberProperties = type.memberProperties.associateBy { it.name }
      val memoizedPropertyMaps = mutableMapOf<KClass<*>, Map<String, KProperty<*>>>(type to memberProperties)

      val subjectParams = mutableSetOf<PropertyInfo>()

      val otherParams = ImmutableMultimap.builder<IRI, PropertyInfo>()
      for (parameter in constructor.parameters) {
        val propertyInfo = PropertyInfo(parameter, memberProperties.getValue(parameter.name!!))

        if (isAnnotationPresent<RdfSubject>(memoizedPropertyMaps, type, parameter)) {
          subjectParams.add(propertyInfo)
          continue
        }

        val predicateIriString =
          findAnnotation<RdfValue>(memoizedPropertyMaps, type, parameter)?.turtleRdfIri
            ?: "aff4:${parameter.name}"
        val predicate = namespacesProvider.iriFromTurtle(predicateIriString.toTurtleIri())

        otherParams.put(predicate, propertyInfo)
      }

      val rdfType = (type.findAnnotation<RdfModel>() ?: constructor.findAnnotation())!!.rdfType
      return RdfAnnotationTypeInfo(
        klass = type,
        rdfType = namespacesProvider.iriFromTurtle(rdfType.toTurtleIri()),
        subjectProperties = subjectParams,
        otherProperties = otherParams.build(),
      )
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
