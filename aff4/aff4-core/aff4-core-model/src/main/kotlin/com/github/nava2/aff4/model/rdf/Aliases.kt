package com.github.nava2.aff4.model.rdf

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory

typealias Aff4Arn = IRI

fun ValueFactory.createArn(value: String): Aff4Arn = createIRI(value)

fun ValueFactory.createArn(namespace: String, localName: String): Aff4Arn = createIRI(namespace, localName)
