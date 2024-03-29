package net.navatwo.kaff4.streams.map_stream

import jakarta.inject.Inject
import net.navatwo.kaff4.Aff4ImageTestModule
import net.navatwo.kaff4.BaseLinear
import net.navatwo.kaff4.UnderTest
import net.navatwo.kaff4.model.Aff4Model
import net.navatwo.kaff4.model.rdf.Aff4Schema
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.test.GuiceModule
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MapIdxFileReaderTest {

  @GuiceModule
  val imageTestModule = Aff4ImageTestModule.BaseLinear

  @Inject
  @field:UnderTest
  private lateinit var aff4Model: Aff4Model

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var mapIdxFileReader: MapIdxFileReader

  private lateinit var mapStream: MapStream

  @BeforeEach
  fun setup() {
    val mapStreamIri = valueFactory.createIRI("aff4://fcbfdce7-4488-4677-abf6-08bc931e195b")
    mapStream = aff4Model.get(mapStreamIri, MapStream::class)
  }

  @Test
  fun `load existing map targets`() {
    val targets = mapIdxFileReader.loadTargets(mapStream)
    assertThat(targets).containsExactly(
      valueFactory.createIRI("aff4://c215ba20-5648-4209-a793-1f918c723610"),
      valueFactory.createIRI(Aff4Schema.SCHEMA, "Zero"),
      valueFactory.createIRI(Aff4Schema.SCHEMA, "SymbolicStreamFF"),
      valueFactory.createIRI(Aff4Schema.SCHEMA, "SymbolicStream61"),
    )

    assertThat(mapIdxFileReader.loadTargets(mapStream)).isEqualTo(targets)
  }
}
