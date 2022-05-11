package com.github.nava2.aff4.meta

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.ForImages
import com.github.nava2.aff4.ForResources
import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.io.sourceProvider
import com.github.nava2.aff4.meta.parser.RdfProvider
import com.github.nava2.aff4.meta.parser.TestRdfRepositoryModule
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.test.GuiceTestRule
import com.google.inject.Provides
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.openZip
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton

class RdfProviderTest {
  @get:Rule
  val rule: GuiceTestRule = GuiceTestRule(
    Aff4CoreModule,
    TestRdfRepositoryModule,
    object : KAbstractModule() {
      override fun configure() = Unit

      @Provides
      @Singleton
      @ForImages
      fun providesFileSystemForImages(@ForResources resourcesFileSystem: FileSystem): FileSystem {
        return resourcesFileSystem.relativeTo("images".toPath())
      }
    }
  )

  @Inject
  @field:ForImages
  private lateinit var imagesFileSystem: FileSystem

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var rdfProvider: RdfProvider

  private lateinit var aff4FileSystem: FileSystem
  private val turtlePath = "information.turtle".toPath()

  @Before
  fun setup() {
    aff4FileSystem = imagesFileSystem.openZip("Base-Linear.aff4".toPath())
  }

  @Test
  fun `parse turtle rdf from zip content`() {
    val consumer = CollectingModelConsumer()
    rdfProvider.parseStream(turtlePath, aff4FileSystem.sourceProvider(turtlePath), consumer)

    assertThat(consumer.models).hasSize(2)
    assertThat(consumer.models).containsOnlyOnce(
      BlockHashes(
        arn = valueFactory.createIRI("aff4://c215ba20-5648-4209-a793-1f918c723610/", "blockhash.md5"),
        hash = Hash.Sha512(
          (
            "9062f1c9f48438a6875a60b7e1323151e8ff583c8531ca7806d6c29b7d961ceddba8783e8e4c49ff37702304cd" +
              "f1dc4c7a9b8f67c73af07fc14422c0be9ae20d"
            ).decodeHex()
        ),
      ),
      BlockHashes(
        arn = valueFactory.createIRI("aff4://c215ba20-5648-4209-a793-1f918c723610/", "blockhash.sha1"),
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
