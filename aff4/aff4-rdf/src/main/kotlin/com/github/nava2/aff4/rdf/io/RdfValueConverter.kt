package com.github.nava2.aff4.rdf.io

import com.google.inject.TypeLiteral
import org.eclipse.rdf4j.model.Value

abstract class RdfValueConverter<T>(vararg types: TypeLiteral<*>) {
  val types = types.toSet()

  abstract fun matches(clazz: Class<*>): Boolean

  abstract fun convert(clazz: Class<*>, value: Value): T?
}

abstract class ConcreteRdfValueConverter<T>(
  vararg types: TypeLiteral<*>,
) : RdfValueConverter<T>(types = types) {

  abstract fun convert(value: Value): T?

  final override fun matches(clazz: Class<*>) = TypeLiteral.get(clazz) in types

  final override fun convert(clazz: Class<*>, value: Value): T? {
    return convert(value)
  }
}
