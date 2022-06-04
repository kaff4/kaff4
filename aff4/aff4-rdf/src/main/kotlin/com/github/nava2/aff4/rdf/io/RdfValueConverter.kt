package com.github.nava2.aff4.rdf.io

import com.google.inject.TypeLiteral
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject

abstract class RdfValueConverter<T> protected constructor(val types: Set<TypeLiteral<*>>) {
  init {
    require(types.isNotEmpty()) { "Must define at least one type matching" }
  }

  @Inject
  protected lateinit var valueFactory: ValueFactory

  abstract fun matches(clazz: Class<*>): Boolean

  abstract fun parse(clazz: Class<*>, value: Value): T?

  abstract fun serialize(clazz: Class<*>, value: T): Value?

  protected constructor(type0: TypeLiteral<*>, vararg types: TypeLiteral<*>) : this(setOf(type0) + types)
}
