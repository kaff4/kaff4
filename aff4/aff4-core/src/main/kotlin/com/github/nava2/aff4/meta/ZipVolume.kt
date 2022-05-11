package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.rdf.RdfConnectionScoped
import okio.FileHandle
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import java.time.ZonedDateTime
import javax.inject.Inject

data class ZipVolume(
  val iri: Iri,
  val contains: List<Iri>,
  val creationTime: ZonedDateTime,
  val interfaceType: Iri, // todo this should be an enum?
  val stored: FileHandle,
) : Aff4Model {
  @RdfConnectionScoped
  class Parser @Inject constructor(
//    @ForImageFolder private val imageRootFileSystem: FileSystem,
  ) : Aff4Model.Parser<ZipVolume>(types = listOf("aff4:ZipVolume")) {
    override fun protectedTryCreate(subject: Resource, statements: List<Statement>): ZipVolume {
      TODO("Not yet implemented")
    }
  }
}
