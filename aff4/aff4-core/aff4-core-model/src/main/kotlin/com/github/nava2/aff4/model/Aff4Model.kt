package com.github.nava2.aff4.model

import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.ZipVolume
import okio.FileSystem
import okio.Path
import org.eclipse.rdf4j.model.IRI
import kotlin.reflect.KClass

interface Aff4Model : AutoCloseable {
  val imageRootFileSystem: FileSystem
  val containerArn: IRI
  val container: ZipVolume?
  val metadata: Metadata

  fun <T : Aff4RdfModel> query(modelType: KClass<T>): List<T>

  fun <T : Aff4RdfModel> get(subject: IRI, modelType: KClass<T>): T

  fun <T : Aff4RdfModel> querySubjectStartsWith(subjectPrefix: String, modelType: KClass<T>): List<T>

  interface Loader {
    fun load(fileSystem: FileSystem, path: Path): Aff4Model
  }

  data class Metadata(
    val version: String,
    val tool: String,
  )
}
