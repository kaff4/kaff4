package net.navatwo.kaff4.model.rdf

import org.eclipse.rdf4j.model.ValueFactory

fun ValueFactory.createArn(value: String): Aff4Arn = createIRI(value)

fun ValueFactory.createArn(namespace: String, localName: String): Aff4Arn = createIRI(namespace, localName)
