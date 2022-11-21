package com.github.nava2.aff4.model

import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.ZipVolume
import com.github.nava2.aff4.model.rdf.annotations.RdfModel
import com.github.nava2.aff4.rdf.RdfConnection
import com.github.nava2.aff4.rdf.RdfExecutor
import com.github.nava2.aff4.rdf.io.RdfModelParser
import com.github.nava2.aff4.rdf.querySubjectsByType
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.FileSystem
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

internal class RealAff4Model @AssistedInject constructor(
  private val rdfExecutor: RdfExecutor,
  private val valueFactory: ValueFactory,
  private val rdfModelParser: RdfModelParser,
  @Assisted override val containerContext: Aff4ContainerContext,
) : Aff4Model {
  override val imageRootFileSystem: FileSystem get() = containerContext.imageFileSystem
  override val containerArn: Aff4Arn get() = containerContext.containerArn

  @Volatile
  private var closed = false

  private val modelArns = mutableMapOf<KClass<*>, String>()

  private val _container: ZipVolume? by lazy {
    get(containerArn, ZipVolume::class)
  }

  override val container: ZipVolume?
    get() {
      check(!closed) { "Closed" }
      return _container
    }

  override fun <T : Aff4RdfModel> query(modelType: KClass<T>): List<T> {
    return query { connection ->
      val modelRdfType = getModelRdfType(modelType)
      val subjects = connection.querySubjectsByType(connection.namespaces.iriFromTurtle(modelRdfType))

      subjects.map { subject ->
        val statements = connection.queryStatements(subj = subject).use { it.toList() }
        rdfModelParser.parse(connection, modelType, subject, statements)
      }
    }
  }

  override fun <T : Aff4RdfModel> get(subject: IRI, modelType: KClass<T>): T {
    return query { connection ->
      val statements = connection.queryStatements(subj = subject).use { it.toList() }
      rdfModelParser.parse(connection, modelType, subject, statements)
    }
  }

  override fun <T : Aff4RdfModel> querySubjectStartsWith(subjectPrefix: String, modelType: KClass<T>): List<T> {
    return query { connection ->
      val q = connection.prepareGraphQuery(
        """
          CONSTRUCT { ?s ?p ?o }
          WHERE {
              ?s ?p ?o .
            FILTER (strstarts(str(?s), ?q))
          }
          LIMIT 1000
        """.trimIndent()
      )

      q.setBinding("q", valueFactory.createLiteral(subjectPrefix))

      val statementsBySubject = q.evaluate().use { r -> r.toList() }.groupBy { it.subject }

      statementsBySubject.entries.map { (subject, statements) ->
        rdfModelParser.parse(connection, modelType, subject, statements)
      }
    }
  }

  override fun close() {
    if (closed) return
    closed = true
  }

  private inline fun <T> query(crossinline block: (connection: RdfConnection) -> T): T {
    check(!closed) { "Closed" }

    return rdfExecutor.withReadOnlySession<T> { block(it) }
  }

  private fun getModelRdfType(modelType: KClass<*>) =
    modelArns.getOrPut(modelType) { modelType.findAnnotation<RdfModel>()!!.rdfType }

  internal interface AssistedFactory {
    fun create(containerContext: Aff4ContainerContext): RealAff4Model
  }

  @Singleton
  internal class Loader @Inject constructor(
    private val assistedFactory: AssistedFactory,
  ) : Aff4Model.Loader {
    override fun load(containerContext: Aff4ContainerContext): RealAff4Model {
      loadTurtle(containerContext.containerArn, containerContext.rdfExecutor, containerContext.imageFileSystem)
      return assistedFactory.create(containerContext = containerContext)
    }

    private fun loadTurtle(containerArn: Aff4Arn, rdfExecutor: RdfExecutor, imageFileSystem: FileSystem) {
      rdfExecutor.withReadWriteSession { connection ->
        imageFileSystem.read("information.turtle".toPath()) {
          inputStream().use { connection.addTurtle(containerArn, it) }
        }
      }
    }
  }
}
