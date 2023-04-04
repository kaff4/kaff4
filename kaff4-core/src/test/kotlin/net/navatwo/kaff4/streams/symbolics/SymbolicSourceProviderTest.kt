package net.navatwo.kaff4.streams.symbolics

import net.navatwo.kaff4.Aff4ImageTestModule
import net.navatwo.kaff4.BaseLinear
import net.navatwo.kaff4.io.buffer
import net.navatwo.kaff4.io.limit
import net.navatwo.kaff4.io.repeatByteString
import net.navatwo.kaff4.io.use
import net.navatwo.test.GuiceModule
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.Test
import javax.inject.Inject

class SymbolicSourceProviderTest {
  @GuiceModule
  val imageTestModule = Aff4ImageTestModule.BaseLinear

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Test
  fun `setting fixed length causes source to be limited`() {
    val iri = valueFactory.createIRI("http://aff4.org/schema", "Zero")
    val sourceProvider = SymbolicSourceProvider(iri, 0.repeatByteString(1), 1024)
    sourceProvider.limit(10).buffer().use { source ->
      assertThat(source.readByteString()).isEqualTo(0.repeatByteString(10))
    }
  }

  @Test
  fun `setting no fixed length causes source to be unlimited`() {
    val iri = valueFactory.createIRI("http://aff4.org/schema", "SymbolicStreamFF")
    val sourceProvider = SymbolicSourceProvider(iri, 0xff.repeatByteString(1), 1024)

    val reasonableButAlsoWayBiggerThanBufferWouldEverBeSize = 1 * 1024 * 1024 // 1MiB
    sourceProvider.buffer().use { source ->
      assertThat(source.readByteString(reasonableButAlsoWayBiggerThanBufferWouldEverBeSize.toLong()))
        .isEqualTo(0xff.repeatByteString(reasonableButAlsoWayBiggerThanBufferWouldEverBeSize))
    }
  }
}
