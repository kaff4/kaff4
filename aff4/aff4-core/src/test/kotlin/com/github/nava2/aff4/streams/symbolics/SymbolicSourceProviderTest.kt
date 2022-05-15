package com.github.nava2.aff4.streams.symbolics

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.ForImages
import com.github.nava2.aff4.ForResources
import com.github.nava2.aff4.io.relativeTo
import com.github.nava2.aff4.meta.rdf.MemoryRdfRepositoryConfiguration
import com.github.nava2.aff4.meta.rdf.RdfRepositoryConfiguration
import com.github.nava2.aff4.streams.Aff4StreamsModule
import com.github.nava2.aff4.streams.compression.SnappyModule
import com.github.nava2.aff4.streams.repeatByteString
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.test.GuiceTestRule
import com.google.inject.Provides
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton

class SymbolicSourceProviderTest {
  @get:Rule
  val rule: GuiceTestRule = GuiceTestRule(
    Aff4CoreModule,
    Aff4StreamsModule,
    SnappyModule,
    object : KAbstractModule() {
      override fun configure() {
        bind<RdfRepositoryConfiguration>().toInstance(MemoryRdfRepositoryConfiguration)
      }

      @Provides
      @Singleton
      @ForImages
      fun providesFileSystemForImages(@ForResources resourcesFileSystem: FileSystem): FileSystem {
        return resourcesFileSystem.relativeTo("images".toPath())
      }
    }
  )

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Test
  fun `setting fixed length causes source to be limited`() {
    val iri = valueFactory.createIRI("http://aff4.org/schema", "Zero")
    val sourceProvider = SymbolicSourceProvider(iri, 0.repeatByteString(1), 1024)
    sourceProvider.fixed(10).buffer().use { source ->
      Buffer().use { buffer ->
        source.readAll(buffer)
        assertThat(buffer.readByteString()).isEqualTo(0.repeatByteString(10))
      }
    }
  }

  @Test
  fun `setting no fixed length causes source to be unlimited`() {
    val iri = valueFactory.createIRI("http://aff4.org/schema", "SymbolicStreamFF")
    val sourceProvider = SymbolicSourceProvider(iri, 0xff.repeatByteString(1), 1024)

    val reasonableButAlsoWayBiggerThanBufferWouldEverBeSize = 1 * 1024 * 1024 // 1MiB
    sourceProvider.infinite().buffer().use { source ->
      Buffer().use { buffer ->
        source.readFully(buffer, reasonableButAlsoWayBiggerThanBufferWouldEverBeSize.toLong())
        assertThat(buffer.readByteString())
          .isEqualTo(0xff.repeatByteString(reasonableButAlsoWayBiggerThanBufferWouldEverBeSize))
      }
    }
  }
}
