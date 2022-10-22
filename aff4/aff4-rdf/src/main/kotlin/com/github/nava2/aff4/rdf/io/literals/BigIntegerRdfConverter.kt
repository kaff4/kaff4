package com.github.nava2.aff4.rdf.io.literals

import com.github.nava2.aff4.rdf.io.ConcreteRdfValueConverter
import com.github.nava2.guice.typeLiteral
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BigIntegerRdfConverter @Inject constructor()
  : ConcreteRdfValueConverter<BigInteger>(typeLiteral<BigInteger>()) {
  override fun parse(value: Value): BigInteger? = (value as? Literal)?.integerValue()
  override fun serialize(value: BigInteger): Value = valueFactory.createLiteral(value)
}
