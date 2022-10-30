package com.github.nava2.aff4.model

import com.github.nava2.aff4.Aff4ImageTestModule
import com.github.nava2.aff4.UnderTest
import com.github.nava2.aff4.io.md5
import com.github.nava2.aff4.io.use
import com.github.nava2.aff4.model.Aff4Container.ToolMetadata
import com.github.nava2.aff4.streams.compression.Aff4SnappyModule
import com.github.nava2.test.GuiceExtension
import com.github.nava2.test.GuiceModule
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.inject.Inject

@ExtendWith(GuiceExtension::class)
class Aff4ModelNestedLogicalImagesTest {
  @GuiceModule
  val imageTestModule = Aff4ImageTestModule(imageName = "nested_logical_images.aff4", Aff4SnappyModule)

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  @field:UnderTest
  private lateinit var aff4Container: Aff4Container

  @Inject
  @field:UnderTest
  private lateinit var aff4Model: Aff4Model

  @Inject
  @field:UnderTest
  private lateinit var aff4StreamOpener: Aff4StreamOpener

  @Test
  fun `model loads correctly`() {
    assertThat(aff4Container.metadata).isEqualTo(ToolMetadata("1.1", "pyaff4"))
    assertThat(aff4Model.containerArn).isEqualTo(arn("aff4://2872342b-8aff-4747-9325-6cd5f50bcff5"))
  }

  @Test
  fun `loads files`() {
    val unicodeTxt = arn(
      "aff4://2872342b-8aff-4747-9325-6cd5f50bcff5/test_images.zip/test_images/AFF4-L/ネコ.txt"
    )
    val unicodeTxtStreamProvider = aff4StreamOpener.openStream(unicodeTxt)
    unicodeTxtStreamProvider.use {
      assertThat(it).md5(4, "d3b07384d113edec49eaa6238ad5ff00")
    }

    (unicodeTxtStreamProvider as VerifiableStreamProvider).verify(aff4Model)

    val dreamTxtIri = arn(
      "aff4://2872342b-8aff-4747-9325-6cd5f50bcff5/test_images.zip/test_images/AFF4-L/dream.txt"
    )
    val dreamTxtStreamProvider = aff4StreamOpener.openStream(dreamTxtIri)
    dreamTxtStreamProvider.use {
      assertThat(it).md5(8688, "75d83773f8d431a3ca91bfb8859e486d")
    }

    (dreamTxtStreamProvider as VerifiableStreamProvider).verify(aff4Model)

    val dreamAff4Iri = arn(
      "aff4://2872342b-8aff-4747-9325-6cd5f50bcff5/test_images.zip/test_images/AFF4-L/dream.aff4"
    )
    val dreamAff4StreamProvider = aff4StreamOpener.openStream(dreamAff4Iri)
    dreamAff4StreamProvider.use {
      assertThat(it).md5(4542, "082d02390c1c4de5af617afa4f467258")
    }

    (dreamAff4StreamProvider as VerifiableStreamProvider).verify(aff4Model)
  }

  private fun arn(iri: String) = valueFactory.createIRI(iri)
}
