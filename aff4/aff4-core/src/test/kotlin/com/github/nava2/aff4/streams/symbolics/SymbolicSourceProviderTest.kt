package com.github.nava2.aff4.streams.symbolics

import com.github.nava2.aff4.Aff4ImageTestRule
import com.github.nava2.aff4.streams.repeatByteString
import okio.Buffer
import okio.buffer
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

class SymbolicSourceProviderTest {
  @get:Rule
  val rule: Aff4ImageTestRule = Aff4ImageTestRule()

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
