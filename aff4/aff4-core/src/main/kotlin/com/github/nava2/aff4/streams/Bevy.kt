package com.github.nava2.aff4.streams

import okio.Path
import okio.Path.Companion.toPath
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
import java.net.URLEncoder
import javax.inject.Inject

private const val BEVY_FILENAME_PADDING_LENGTH = 8

internal data class Bevy(
  val arn: IRI,
  val index: Int,
  val dataSegment: Path,
  val indexSegment: Path,
) {
  class Factory @Inject constructor(
    private val valueFactory: ValueFactory,
  ) {
    fun create(
      imageArn: IRI,
      index: Int,
    ): Bevy {
      val indexId = indexId(index)
      val arn = valueFactory.createIRI("$imageArn/$indexId")
      val imagePath = URLEncoder.encode(imageArn.stringValue(), Charsets.UTF_8).toPath()
      return Bevy(
        arn = arn,
        index = index,
        dataSegment = imagePath / indexId,
        indexSegment = imagePath / "$indexId.index",
      )
    }

    private fun indexId(index: Int) = index.toString().padStart(BEVY_FILENAME_PADDING_LENGTH, '0')
  }
}
