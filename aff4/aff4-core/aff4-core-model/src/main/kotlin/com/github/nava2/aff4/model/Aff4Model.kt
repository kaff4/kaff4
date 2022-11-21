package com.github.nava2.aff4.model

import com.github.nava2.aff4.model.rdf.Aff4Arn
import com.github.nava2.aff4.model.rdf.Aff4RdfModel
import com.github.nava2.aff4.model.rdf.ZipVolume
import okio.FileSystem
import java.io.Closeable
import kotlin.reflect.KClass

interface Aff4Model : Closeable {
  val containerContext: Aff4ContainerContext

  val imageRootFileSystem: FileSystem
  val containerArn: Aff4Arn
  val container: ZipVolume?

  fun <T : Aff4RdfModel> query(modelType: KClass<T>): List<T>

  fun <T : Aff4RdfModel> get(subject: Aff4Arn, modelType: KClass<T>): T

  fun <T : Aff4RdfModel> querySubjectStartsWith(subjectPrefix: String, modelType: KClass<T>): List<T>

  interface Loader {
    fun load(containerContext: Aff4ContainerContext): Aff4Model
  }
}

inline fun <reified T : Aff4RdfModel> Aff4Model.get(subject: Aff4Arn): T = get(subject, T::class)

inline fun <reified T : Aff4RdfModel> Aff4Model.query(): List<T> = query(T::class)
