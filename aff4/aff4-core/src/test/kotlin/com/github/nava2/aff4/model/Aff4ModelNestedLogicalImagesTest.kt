package com.github.nava2.aff4.model

import com.github.nava2.aff4.Aff4LogicalImageTestRule
import com.github.nava2.aff4.io.md5
import com.github.nava2.aff4.streams.compression.SnappyModule
import com.github.nava2.test.GuiceTestRule
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

class Aff4ModelNestedLogicalImagesTest {
  @get:Rule
  val rule: GuiceTestRule = Aff4LogicalImageTestRule("nested_logical_images.aff4", SnappyModule)

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var aff4Model: Aff4Model

  @Inject
  private lateinit var aff4StreamOpener: Aff4StreamOpener

  @Test
  fun `model loads correctly`() {
    assertThat(aff4Model.metadata).isEqualTo(Aff4Model.Metadata("1.1", "pyaff4"))
    assertThat(aff4Model.containerArn).isEqualTo(arn("aff4://2872342b-8aff-4747-9325-6cd5f50bcff5"))
  }

  @Test
  fun `loads files`() {
    val unicodeTxt = arn(
      "aff4://2872342b-8aff-4747-9325-6cd5f50bcff5/test_images.zip/test_images/AFF4-L/ネコ.txt"
    )
    aff4StreamOpener.openStream(unicodeTxt).use { stream ->
      stream.source(0).use {
        assertThat(it).md5(4, "d3b07384d113edec49eaa6238ad5ff00")
      }

      (stream as VerifiableStream).verify(aff4Model)
    }

    val dreamTxtIri = arn(
      "aff4://2872342b-8aff-4747-9325-6cd5f50bcff5/test_images.zip/test_images/AFF4-L/dream.txt"
    )
    aff4StreamOpener.openStream(dreamTxtIri).use { mapStream ->
      mapStream.source(0).use {
        assertThat(it).md5(8688, "75d83773f8d431a3ca91bfb8859e486d")
      }

      (mapStream as VerifiableStream).verify(aff4Model)
    }

    val dreamAff4Iri = arn(
      "aff4://2872342b-8aff-4747-9325-6cd5f50bcff5/test_images.zip/test_images/AFF4-L/dream.aff4"
    )
    aff4StreamOpener.openStream(dreamAff4Iri).use { mapStream ->
      mapStream.source(0).use {
        assertThat(it).md5(4542, "082d02390c1c4de5af617afa4f467258")
      }

      (mapStream as VerifiableStream).verify(aff4Model)
    }
  }

  private fun arn(iri: String) = valueFactory.createIRI(iri)
}
