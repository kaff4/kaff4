package com.github.nava2.aff4.streams

import com.github.nava2.aff4.meta.rdf.toAff4Path
import okio.Path
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.ValueFactory
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
      val imagePath = imageArn.toAff4Path()
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
