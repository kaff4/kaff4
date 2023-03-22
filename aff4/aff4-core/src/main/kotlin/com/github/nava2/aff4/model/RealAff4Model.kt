package com.github.nava2.aff4.model

import com.github.nava2.aff4.model.dialect.ToolDialect
import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.TurtleIri
import com.github.nava2.aff4.model.rdf.evaluateSequence
import com.github.nava2.aff4.rdf.RdfConnection
import com.github.nava2.aff4.rdf.RdfExecutor
import com.github.nava2.aff4.rdf.io.RdfModelParser
import com.github.nava2.aff4.rdf.querySubjectsByType
import com.github.nava2.guice.action_scoped.ActionScoped
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.query.GraphQuery
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

private val PATTERN_ORDER_BY = Regex("\\s+order by\\s+", RegexOption.IGNORE_CASE)

private const val CONNECTION_CHUNK_SIZE = 4096

internal class RealAff4Model @AssistedInject constructor(
  private val rdfExecutor: RdfExecutor,
  private val valueFactory: ValueFactory,
  private val rdfModelParser: RdfModelParser,
  @ActionScoped private val toolDialect: ToolDialect,
  @Assisted override val containerContext: Aff4ImageContext,
) : Aff4Model {
  @Volatile
  private var closed = false

  override fun <T : Aff4RdfModel> query(modelType: KClass<T>): Sequence<T> {
    val modelRdfTypes = getModelRdfTypes(modelType)
    val bindings = mutableMapOf<String, Resource>()

    return sequence {
      for (modelRdfType in modelRdfTypes) {
        yieldAll(
          queryPaginatedSubjects(modelRdfType, bindings, modelType)
        )
      }
    }
  }

  private fun <T : Aff4RdfModel> queryPaginatedSubjects(
    modelRdfType: TurtleIri,
    bindings: MutableMap<String, Resource>,
    modelType: KClass<T>
  ): Sequence<T> {
    val queryProvider = object : (RdfConnection) -> String {
      private lateinit var querySubjects: List<Resource>

      @Synchronized
      private fun getSubjects(connection: RdfConnection): List<Resource> {
        if (!::querySubjects.isInitialized) {
          querySubjects = connection.querySubjectsByType(connection.namespaces.iriFromTurtle(modelRdfType))
        }
        return querySubjects
      }

      override fun invoke(connection: RdfConnection): String {
        val subjects = getSubjects(connection)

        return buildString {
          appendLine(
            """
            CONSTRUCT { ?s ?p ?o }
            WHERE {
              ?s ?p ?o .
            """.trimIndent()
          )

          append("    FILTER( ?s IN (")
            .append(
              subjects.joinToString { subj ->
                val binding = "subj_%03d".format(bindings.size)
                bindings[binding] = subj
                "?$binding"
              }
            )
            .appendLine("))")

          appendLine('}')
          appendLine("ORDER BY ?s ?p ?o")
        }
      }
    }

    return paginated(
      queryProvider = queryProvider,
      bindingsProvider = {
        for ((binding, value) in bindings) {
          setBinding(binding, value)
        }
      },
      consumeStatements = { connection, subject, statements ->
        rdfModelParser.parse(
          rdfConnection = connection,
          type = modelType,
          subject = subject,
          statements = statements,
        )
      }
    )
  }

  override fun <T : Aff4RdfModel> get(subject: IRI, modelType: KClass<T>): T {
    return query { connection ->
      val statements = connection.queryStatements(subj = subject).use { it.toList() }
      rdfModelParser.parse(connection, modelType, subject, statements)
    }
  }

  private fun <R> paginated(
    queryProvider: (connection: RdfConnection) -> String,
    bindingsProvider: GraphQuery.(connection: RdfConnection) -> Unit = {},
    consumeStatements: (connection: RdfConnection, subject: Resource, statements: Collection<Statement>) -> R,
  ): Sequence<R> {
    return sequence {
      var offset = 0

      val statementBuffer = mutableListOf<Statement>()

      do {
        val (results, queryResultCount) = query { connection ->
          val query = queryProvider(connection)
          check(PATTERN_ORDER_BY in query) {
            "Must have 'ORDER BY' clause: $query"
          }

          val q = connection.prepareGraphQuery(
            """
            $query
            LIMIT $CONNECTION_CHUNK_SIZE
            OFFSET $offset
            """.trimIndent()
          )

          q.bindingsProvider(connection)

          var queryResultCount = 0

          val results = mutableListOf<R>()
          for (statement in q.evaluateSequence()) {
            queryResultCount += 1

            val lastStatement = statementBuffer.lastOrNull()
            if (lastStatement?.subject != statement.subject) {
              if (lastStatement != null) {
                results += consumeStatements(connection, lastStatement.subject, statementBuffer)
                statementBuffer.clear()
              }
            }

            statementBuffer += statement
          }

          // Often we don't fill the full chunk size, so we should avoid an extra query
          if (queryResultCount < CONNECTION_CHUNK_SIZE && statementBuffer.isNotEmpty()) {
            val firstStatement = statementBuffer.first()
            results += consumeStatements(connection, firstStatement.subject, statementBuffer)
            statementBuffer.clear()
          }

          offset += queryResultCount
          results to queryResultCount
        }

        yieldAll(results)
      } while (queryResultCount > 0)

      if (statementBuffer.isNotEmpty()) {
        val lastResult = query { connection ->
          consumeStatements(connection, statementBuffer.first().subject, statementBuffer)
        }
        yield(lastResult)
      }
    }
  }

  override fun <T : Aff4RdfModel> querySubjectStartsWith(subjectPrefix: String, modelType: KClass<T>): Sequence<T> {
    return paginated(
      queryProvider = {
        """
            CONSTRUCT { ?s ?p ?o }
            WHERE {
              ?s ?p ?o .
                FILTER (strstarts(str(?s), ?q))
            }
            ORDER BY ?s ?p ?o
        """.trimIndent()
      },
      bindingsProvider = {
        setBinding("q", valueFactory.createLiteral(subjectPrefix))
      },
      consumeStatements = { connection, subject, statements ->
        rdfModelParser.parse(
          rdfConnection = connection,
          type = modelType,
          subject = subject,
          statements = statements,
        )
      }
    )
  }

  override fun close() {
    if (closed) return
    closed = true
  }

  private inline fun <T> query(crossinline block: (connection: RdfConnection) -> T): T {
    check(!closed) { "Closed" }

    return rdfExecutor.withReadOnlySession { block(it) }
  }

  private fun getModelRdfTypes(modelType: KClass<*>): Collection<TurtleIri> {
    return toolDialect.typeResolver.getAll(modelType)
  }

  internal interface AssistedFactory {
    fun create(containerContext: Aff4ImageContext): RealAff4Model
  }

  @Singleton
  internal class Loader @Inject constructor(
    private val assistedFactory: AssistedFactory,
  ) : Aff4Model.Loader {
    override fun load(imageContext: Aff4ImageContext): RealAff4Model {
      loadTurtles(imageContext.rdfExecutor, imageContext.containers)
      return assistedFactory.create(containerContext = imageContext)
    }

    private fun loadTurtles(rdfExecutor: RdfExecutor, containers: Collection<Aff4Container>) {
      rdfExecutor.withReadWriteSession { connection ->
        for (container in containers) {
          container.dataFileSystem.read("information.turtle".toPath()) {
            inputStream().use { connection.addTurtle(container.containerArn, it) }
          }
        }
      }
    }
  }
}
