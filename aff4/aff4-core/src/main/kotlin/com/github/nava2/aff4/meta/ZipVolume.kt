package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.parser.ForImageFolder
import okio.FileHandle
import okio.FileSystem
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class ZipVolume(
  val iri: Iri,
  val contains: List<Iri>,
  val creationTime: ZonedDateTime,
  val interfaceType: Iri, // todo this should be an enum?
  val stored: FileHandle,
) : Aff4Model {
  @Singleton
  class Parser @Inject constructor(
    @ForImageFolder private val imageRootFileSystem: FileSystem,
  ) : Aff4Model.Parser(types = listOf("aff4:ZipVolume")) {
    override fun protectedTryCreate(context: ModelRdfContext): ZipVolume {
      context.statements.first()

      TODO()
    }
  }
}
