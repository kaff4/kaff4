package com.github.nava2.aff4.model.dialect

import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.TurtleIri
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations

interface DialectTypeResolver {
  val primaryBindings: ImmutableMap<KClass<out Aff4RdfModel>, TurtleIri>
  val bindings: ImmutableListMultimap<KClass<out Aff4RdfModel>, TurtleIri>

  operator fun get(klass: KClass<out Aff4RdfModel>): TurtleIri?
  fun getValue(klass: KClass<out Aff4RdfModel>): TurtleIri {
    return get(klass)
      ?: throw NoSuchElementException("Key $klass does not exist")
  }

  operator fun get(iri: TurtleIri): KClass<out Aff4RdfModel>?
  fun getValue(iri: TurtleIri): KClass<out Aff4RdfModel>? {
    return get(iri)
      ?: throw NoSuchElementException("Key $iri does not exist")
  }

  object Builder {
    fun empty(): SimpleBuilder = SimpleBuilder()

    fun withExisting(typeResolver: DialectTypeResolver): SimpleBuilder = SimpleBuilder(typeResolver)

    fun <A : Annotation> forAnnotation(
      annotationKlass: KClass<A>,
      extractor: A.() -> Pair<String, Set<String>>,
    ): AnnotationBasedBuilder<A> = AnnotationBasedBuilder(annotationKlass, extractor)
  }

  open class BuilderBase<SELF : BuilderBase<SELF>> internal constructor() {
    private val primaryMappings = mutableMapOf<KClass<out Aff4RdfModel>, TurtleIri>()
    private val mappings = ArrayListMultimap.create<KClass<out Aff4RdfModel>, TurtleIri>()

    protected fun register(
      klass: KClass<out Aff4RdfModel>,
      primaryValue: TurtleIri,
      additionalValues: Collection<TurtleIri>,
    ): SELF {
      primaryMappings[klass] = primaryValue
      val values = buildList(1 + additionalValues.size) {
        add(primaryValue)
        addAll(additionalValues)
      }.distinct()

      mappings.putAll(klass, values)

      @Suppress("UNCHECKED_CAST")
      return this as SELF
    }

    fun remove(klass: KClass<out Aff4RdfModel>): TurtleIri? {
      mappings.removeAll(klass)
      return primaryMappings.remove(klass)
    }

    fun build(): DialectTypeResolver {
      return ImmutableTypeResolver(
        primaryBindings = ImmutableMap.copyOf(primaryMappings),
        bindings = ImmutableListMultimap.copyOf(mappings),
      )
    }

    private data class ImmutableTypeResolver(
      override val primaryBindings: ImmutableMap<KClass<out Aff4RdfModel>, TurtleIri>,
      override val bindings: ImmutableListMultimap<KClass<out Aff4RdfModel>, TurtleIri>,
    ) : DialectTypeResolver {
      private val inverse by lazy(LazyThreadSafetyMode.NONE) {
        ImmutableMap.builder<TurtleIri, KClass<out Aff4RdfModel>>()
          .apply {
            for ((klass, iri) in bindings.entries()) {
              put(iri, klass)
            }
          }
          .buildOrThrow()
      }

      override fun get(klass: KClass<out Aff4RdfModel>): TurtleIri? = primaryBindings[klass]
      override fun get(iri: TurtleIri): KClass<out Aff4RdfModel>? = inverse[iri]
    }
  }

  class SimpleBuilder internal constructor() : BuilderBase<SimpleBuilder>() {
    fun register(
      klass: KClass<out Aff4RdfModel>,
      primaryValue: String,
      vararg additionalValues: String,
    ): SimpleBuilder {
      return register(klass, TurtleIri(primaryValue), additionalValues.map { TurtleIri(it) })
    }

    operator fun set(klass: KClass<out Aff4RdfModel>, value: String): SimpleBuilder {
      return register(klass, value)
    }

    internal constructor(typeResolver: DialectTypeResolver) : this() {
      for ((klass, iris) in typeResolver.bindings.asMap()) {
        register(klass, iris.first(), iris.drop(1))
      }
    }
  }

  class AnnotationBasedBuilder<A : Annotation> internal constructor(
    private val annotationKlass: KClass<A>,
    private val extractor: A.() -> Pair<String, Set<String>>,
  ) : BuilderBase<AnnotationBasedBuilder<A>>() {
    fun register(klass: KClass<out Aff4RdfModel>): AnnotationBasedBuilder<A> {
      val annotation = checkNotNull(klass.findAnnotations(annotationKlass).singleOrNull()) {
        "Can not register $klass without $annotationKlass present"
      }
      val (primaryIri, synonyms) = annotation.extractor()
      register(klass, TurtleIri(primaryIri), synonyms.map { TurtleIri(it) })
      return this
    }

    inline fun <reified T : Aff4RdfModel> register(): AnnotationBasedBuilder<A> {
      return register(T::class)
    }
  }
}
