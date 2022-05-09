package com.github.nava2.aff4.meta

import com.github.nava2.aff4.io.sourceProvider
import com.github.nava2.test.GuiceTestRule
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.openZip
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

class RdfProviderTest {
  @get:Rule
  val rule: GuiceTestRule = GuiceTestRule(Aff4ModelModule)

  private val aff4FileSystem = FileSystem.RESOURCES.openZip("images/Base-Linear.aff4".toPath())

  @Inject
  private lateinit var rdfProvider: RdfProvider

  @Test
  fun `parse turtle rdf from zip content`() {
    val turtleProvider = aff4FileSystem.sourceProvider("information.turtle".toPath())
    val consumer = CollectingModelConsumer()
    rdfProvider.parseStream(turtleProvider, consumer)
    assertThat(consumer.models).hasSize(2)
    assertThat(consumer.models).containsOnlyOnce(
      BlockHashes(
        iri = Iri("aff4://c215ba20-5648-4209-a793-1f918c723610/", "blockhash.md5"),
        hash = Hash.Sha512(
          (
            "9062f1c9f48438a6875a60b7e1323151e8ff583c8531ca7806d6c29b7d961ceddba8783e8e4c49ff37702304cd" +
              "f1dc4c7a9b8f67c73af07fc14422c0be9ae20d"
            ).decodeHex()
        ),
      ),
      BlockHashes(
        iri = Iri("aff4://c215ba20-5648-4209-a793-1f918c723610/", "blockhash.sha1"),
        hash = Hash.Sha512(
          (
            "5f487386e32230f282174d197c40a6de4b8d039449a90cf0b720aeb9d213cf337b92a6f0547c5150dd5d1dfcc8" +
              "17e6d5018a2383efec7b6df38015235c9be9e1"
            ).decodeHex()
        ),
      )
    )
  }
}
