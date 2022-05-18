package com.github.nava2.aff4.model

import com.github.nava2.aff4.meta.rdf.model.Aff4RdfModel
import com.github.nava2.aff4.meta.rdf.model.ZipVolume
import com.github.nava2.aff4.streams.Aff4Stream
import okio.FileSystem
import okio.Path
import org.eclipse.rdf4j.model.IRI
import kotlin.reflect.KClass

interface Aff4Model : AutoCloseable {
  val imageRootFileSystem: FileSystem
  val container: ZipVolume
  val metadata: Metadata

  fun openStream(iri: IRI): Aff4Stream

  fun <T : Aff4RdfModel> query(modelType: KClass<T>): List<T>
  fun <T : Aff4RdfModel> get(subject: IRI, modelType: KClass<T>): T

  interface Loader {
    fun load(fileSystem: FileSystem, path: Path): Aff4Model
  }

  data class Metadata(
    val version: String,
    val tool: String,
  )
}
