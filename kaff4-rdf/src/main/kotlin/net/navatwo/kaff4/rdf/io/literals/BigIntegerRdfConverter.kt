package net.navatwo.kaff4.rdf.io.literals

import net.navatwo.guice.typeLiteral
import net.navatwo.kaff4.rdf.io.ConcreteRdfValueConverter
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BigIntegerRdfConverter @Inject constructor() :
  ConcreteRdfValueConverter<BigInteger>(typeLiteral<BigInteger>()) {
  override fun parse(value: Value): BigInteger? = (value as? Literal)?.integerValue()
  override fun serialize(value: BigInteger): Value = valueFactory.createLiteral(value)
}
