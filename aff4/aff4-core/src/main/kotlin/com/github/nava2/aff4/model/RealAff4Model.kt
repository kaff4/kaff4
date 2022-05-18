package com.github.nava2.aff4.model

import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.meta.rdf.ForImageRoot
import com.github.nava2.aff4.meta.rdf.NamespacesProvider
import com.github.nava2.aff4.meta.rdf.RdfConnectionScoping
import com.github.nava2.aff4.meta.rdf.ScopedConnection
import com.github.nava2.aff4.meta.rdf.io.RdfModel
import com.github.nava2.aff4.meta.rdf.io.RdfModelParser
import com.github.nava2.aff4.meta.rdf.model.Aff4RdfModel
import com.github.nava2.aff4.meta.rdf.model.ZipVolume
import com.github.nava2.aff4.meta.rdf.querySubjectsByType
import com.github.nava2.aff4.model.Aff4Model.Metadata
import com.github.nava2.aff4.streams.Aff4Stream
import com.github.nava2.aff4.streams.Aff4StreamLoaderContext
import com.github.nava2.aff4.streams.symbolics.Symbolics
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.getInstance
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import com.google.inject.assistedinject.FactoryModuleBuilder
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.rio.RDFFormat
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

internal class RealAff4Model @AssistedInject constructor(
  private val rdfConnectionScoping: RdfConnectionScoping,
  private val modelKlasses: Set<KClass<out Aff4RdfModel>>,
  aff4StreamLoaderContexts: Set<Aff4StreamLoaderContext>,
  private val symbolics: Symbolics,
  @Assisted override val imageRootFileSystem: FileSystem,
  @Assisted private val containerArn: IRI,
  @Assisted override val metadata: Metadata,
) : Aff4Model {
  @Volatile
  private var closed = false

  private val modelIris = mutableMapOf<KClass<*>, String>()

  private val modelKlassesByRdfType by lazy {
    rdfConnectionScoping.scoped { namespaces: NamespacesProvider ->
      modelKlasses.associateBy { klass ->
        val rdfModelType = klass.findAnnotation<RdfModel>()!!.rdfType
        namespaces.iriFromTurtle(rdfModelType)
      }
    }
  }

  private val aff4StreamLoaderContexts = aff4StreamLoaderContexts.associateBy { it.configTypeLiteral }

  // TODO For logical images, this will be _massive_ and not okay. We will need to be smarter about caching
  private val openStreams = ConcurrentHashMap<IRI, Aff4Stream>()

  private val _container: ZipVolume by lazy {
    get(containerArn, ZipVolume::class)
  }

  override val container: ZipVolume
    get() {
      check(!closed) { "Closed" }
      return _container
    }

  override fun openStream(iri: IRI): Aff4Stream {
    check(!closed) { "Closed" }

    val symbolic = symbolics.maybeGetProvider(iri)
    if (symbolic != null) return symbolic

    return openStreams.computeIfAbsent(iri) { key ->
      query { connection, rdfModelParser ->
        val namespaces = connection.namespaces
        val statements = connection.queryStatements(subj = key).use { it.toList() }
        val rdfTypes = statements.asSequence()
          .filter { it.predicate == namespaces.iriFromTurtle("rdf:type") }
          .mapNotNull { it.`object` as? IRI }
          .toSet()

        val modelType = rdfTypes.asSequence().mapNotNull { type -> modelKlassesByRdfType[type] }
          .first { TypeLiteral.get(it.java) in aff4StreamLoaderContexts }

        val rdfModel = rdfModelParser.parse(modelType, iri, statements)
        val streamLoader = aff4StreamLoaderContexts.getValue(TypeLiteral.get(modelType.java)).get()
        streamLoader.load(rdfModel)
      }
    }
  }

  override fun <T : Aff4RdfModel> query(modelType: KClass<T>): List<T> {
    return query { connection, rdfModelParser ->
      val modelRdfType = getModelRdfType(modelType)
      val subjects = connection.querySubjectsByType(connection.namespaces.iriFromTurtle(modelRdfType))

      subjects.map { subject ->
        val statements = connection.queryStatements(subj = subject).use { it.toList() }
        rdfModelParser.parse(modelType, subject, statements)
      }
    }
  }

  override fun <T : Aff4RdfModel> get(subject: IRI, modelType: KClass<T>): T {
    return query { connection, rdfModelParser ->
      val statements = connection.queryStatements(subj = subject).use { it.toList() }
      rdfModelParser.parse(modelType, subject, statements)
    }
  }

  override fun close() {
    if (closed) return
    closed = true

    val streamsToClose = openStreams.run {
      val beforeClear = values.toSet()
      clear()
      beforeClear + values.toSet()
    }

    for (stream in streamsToClose) {
      stream.close()
    }
  }

  private fun <T> query(block: (connection: ScopedConnection, rdfModelParser: RdfModelParser) -> T): T {
    check(!closed) { "Closed" }

    return rdfConnectionScoping.scoped { connection: ScopedConnection, rdfModelParser: RdfModelParser ->
      block(connection, rdfModelParser)
    }
  }

  private fun getModelRdfType(modelType: KClass<*>) =
    modelIris.getOrPut(modelType) { modelType.findAnnotation<RdfModel>()!!.rdfType }

  private interface AssistedFactory {
    fun create(
      imageRootFileSystem: FileSystem,
      containerArn: IRI,
      metadata: Metadata,
    ): RealAff4Model
  }

  @Singleton
  internal class Loader @Inject constructor(
    private val injector: Injector,
  ) : Aff4Model.Loader {
    override fun load(fileSystem: FileSystem, path: Path): RealAff4Model {
      val imageFileSystem = if (path.toFile().extension in setOf("zip", "aff4")) {
        fileSystem.openZip(path)
      } else {
        fileSystem.relativeTo(path)
      }

      val childInjector = injector.createChildInjector(
        object : KAbstractModule() {
          override fun configure() {
            requireBinding(Key.get(FileSystem::class.java, ForImageRoot::class.java))

            install(FactoryModuleBuilder().build(AssistedFactory::class.java))
          }
        }
      )

      val rdfConnectionScoping = childInjector.getInstance<RdfConnectionScoping>()
      val valueFactory = childInjector.getInstance<ValueFactory>()
      val assistedFactory = childInjector.getInstance<AssistedFactory>()

      loadTurtle(rdfConnectionScoping, imageFileSystem)

      val containerArn = imageFileSystem.read("container.description".toPath()) {
        val iri = readString(Charsets.UTF_8)
        valueFactory.createIRI(iri)
      }

      val (version, tool) = imageFileSystem.read("version.txt".toPath()) {
        var major = -1
        var minor = -1
        lateinit var tool: String
        for (line in readString(Charsets.UTF_8).lineSequence().filter { it.isNotEmpty() }) {
          val (tag, value) = line.split('=')
          when (tag) {
            "major" -> major = value.toInt()
            "minor" -> minor = value.toInt()
            "tool" -> tool = value
          }
        }

        check(major >= 0 && minor >= 0) { "Invalid version.txt" }
        "$major.$minor" to tool
      }

      return assistedFactory.create(
        imageRootFileSystem = imageFileSystem,
        containerArn = containerArn,
        metadata = Metadata(version, tool),
      )
    }

    private fun loadTurtle(rdfConnectionScoping: RdfConnectionScoping, imageFileSystem: FileSystem) {
      rdfConnectionScoping.scoped { scopedConnection: ScopedConnection ->
        imageFileSystem.read("information.turtle".toPath()) {
          scopedConnection.mutable.add(inputStream(), RDFFormat.TURTLE)
        }
      }
    }
  }
}
