package net.navatwo.kaff4.rdf.io

import com.google.inject.TypeLiteral
import net.navatwo.kaff4.rdf.RdfValueConverter
import org.eclipse.rdf4j.model.Value

abstract class ConcreteRdfValueConverter<T> protected constructor(
  types: Set<TypeLiteral<*>>,
) : RdfValueConverter<T>(types) {

  abstract fun parse(value: Value): T?

  abstract fun serialize(value: T): Value?

  final override fun matches(clazz: Class<*>) = TypeLiteral.get(clazz) in types

  final override fun parse(clazz: Class<*>, value: Value): T? = parse(value)

  final override fun serialize(clazz: Class<*>, value: T): Value? = serialize(value)

  protected constructor(type0: TypeLiteral<*>, vararg types: TypeLiteral<*>) : this(setOf(type0) + types)
}
