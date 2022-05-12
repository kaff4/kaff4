package com.github.nava2.aff4.meta.rdf.io

import com.google.inject.TypeLiteral
import org.eclipse.rdf4j.model.Value

abstract class RdfValueConverter<T>(vararg types: TypeLiteral<*>) {
  val types = types.toSet()

  abstract fun convert(value: Value): T?
}
