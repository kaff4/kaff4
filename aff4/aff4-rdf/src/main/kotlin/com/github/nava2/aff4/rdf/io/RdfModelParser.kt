package com.github.nava2.aff4.rdf.io

import com.github.nava2.aff4.rdf.QueryableRdfConnection
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import kotlin.reflect.KClass

interface RdfModelParser {
  fun <T : Any> parse(
    rdfConnection: QueryableRdfConnection,
    type: KClass<T>,
    subject: Resource,
    statements: Collection<Statement>,
  ): T
}
