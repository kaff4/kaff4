package com.github.nava2.aff4.rdf

import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.StoredRdfModel
import com.github.nava2.aff4.model.rdf.annotations.RdfModel
import com.github.nava2.aff4.model.rdf.createAff4Iri
import com.github.nava2.aff4.rdf.schema.RdfSchema
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.util.RDFInserter
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf

internal class TurtleReaderAndInserter @AssistedInject constructor(
  aff4ModelClasses: Set<KClass<out Aff4RdfModel>>,
  @Assisted private val containerArn: Aff4Arn,
  @Assisted private val connection: RepositoryConnection,
) : RDFInserter(connection) {
  private val valueFactory = connection.valueFactory
  private val storedPredicate = valueFactory.createAff4Iri("stored")
  private val typePredicate = valueFactory.createIRI(RdfSchema.SCHEMA, "type")

  private val subjects = mutableMapOf<Resource, ModelData>()

  private val modelsRequiringSubjects by lazy(LazyThreadSafetyMode.NONE) {
    aff4ModelClasses.asSequence()
      .filter { StoredRdfModel::class.isSuperclassOf(it) }
      .map { it.findAnnotation<RdfModel>()!!.rdfType }
      .map { valueFactory.createAff4Iri(it.substringAfter(':')) }
      .toSet()
  }

  override fun startRDF() {
    connection.begin()
    super.startRDF()
  }

  override fun handleStatement(st: Statement) {
    super.handleStatement(st)

    subjects.compute(st.subject) { _, old ->
      val predicate = st.predicate
      val isStoredStatement = predicate == storedPredicate
      val typeIri = (st.`object` as? Resource).takeIf { predicate == typePredicate }
      (old ?: ModelData.DEFAULT).run {
        copy(
          modelType = modelType + setOfNotNull(typeIri),
          storedFound = storedFound || isStoredStatement,
        )
      }
    }
  }

  override fun endRDF() {
    val subjectsToPopulate = subjects.entries.asSequence()
      .filter { (_, model) -> !model.storedFound }
      .filter { (_, model) -> model.modelType.any { it in modelsRequiringSubjects } }
      .map { it.key }

    for (subj in subjectsToPopulate) {
      addStatement(subj, storedPredicate, containerArn, contexts.firstOrNull())
    }

    super.endRDF()

    connection.commit()
  }

  interface Factory {
    fun create(containerArn: Aff4Arn, connection: RepositoryConnection): TurtleReaderAndInserter
  }

  private data class ModelData(
    val modelType: Set<Resource>,
    val storedFound: Boolean,
  ) {
    companion object {
      val DEFAULT = ModelData(setOf(), false)
    }
  }
}
