package com.github.nava2.aff4.model

import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.meta.rdf.RdfConnectionScoping
import com.github.nava2.aff4.meta.rdf.ScopedConnection
import com.github.nava2.aff4.meta.rdf.io.RdfModel
import com.github.nava2.aff4.meta.rdf.io.RdfModelParser
import com.github.nava2.aff4.meta.rdf.model.Aff4RdfModel
import com.github.nava2.aff4.meta.rdf.model.ZipVolume
import com.github.nava2.aff4.meta.rdf.parser.ForImageFolder
import com.github.nava2.aff4.meta.rdf.querySubjectsByType
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.getInstance
import com.google.inject.Injector
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
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class Aff4Model @AssistedInject internal constructor(
  private val rdfConnectionScoping: RdfConnectionScoping,
  @ForImageFolder private val fileSystem: FileSystem,
  @Assisted val containerArn: IRI,
  @Assisted("version") val version: String, // todo semver
  @Assisted("tool") val tool: String,
) {
  private val modelIris = mutableMapOf<KClass<*>, String>()

  val container: ZipVolume by lazy {
    get(containerArn, ZipVolume::class)
  }

  fun <T : Aff4RdfModel> query(modelType: KClass<T>): List<T> {
    return query { connection, rdfModelParser ->
      val modelRdfType = getModelRdfType(modelType)
      val subjects = connection.querySubjectsByType(connection.namespaces.iriFromTurtle(modelRdfType))

      subjects.map { subject ->
        val statements = connection.queryStatements(subj = subject).use { it.toList() }
        rdfModelParser.parse(modelType, subject, statements)
      }
    }
  }

  fun <T : Aff4RdfModel> get(subject: IRI, modelType: KClass<T>): T {
    return query { connection, rdfModelParser ->
      val statements = connection.queryStatements(subj = subject).use { it.toList() }
      rdfModelParser.parse(modelType, subject, statements)
    }
  }

  private fun <T> query(block: (connection: ScopedConnection, rdfModelParser: RdfModelParser) -> T): T {
    return rdfConnectionScoping.scoped { connection: ScopedConnection, rdfModelParser: RdfModelParser ->
      block(connection, rdfModelParser)
    }
  }

  private fun getModelRdfType(modelType: KClass<*>) =
    modelIris.getOrPut(modelType) { modelType.findAnnotation<RdfModel>()!!.rdfType }

  private interface AssistedFactory {
    fun create(
      containerArn: IRI,
      @Assisted("version") version: String,
      @Assisted("tool") tool: String,
    ): Aff4Model
  }

  class Loader @Inject internal constructor(
    private val injector: Injector,
  ) {
    fun load(fileSystem: FileSystem, path: Path): Aff4Model {
      val imageFileSystem = if (path.toFile().extension in setOf("zip", "aff4")) {
        fileSystem.openZip(path)
      } else {
        fileSystem.relativeTo(path)
      }

      val childInjector = injector.createChildInjector(
        object : KAbstractModule() {
          override fun configure() {
            bind<FileSystem>()
              .annotatedWith(ForImageFolder::class.java)
              .toInstance(imageFileSystem)
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
        containerArn = containerArn,
        version = version,
        tool = tool,
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