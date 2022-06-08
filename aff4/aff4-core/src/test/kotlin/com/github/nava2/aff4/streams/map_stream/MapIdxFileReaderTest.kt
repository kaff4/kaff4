package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.Aff4ImageTestRule
import com.github.nava2.aff4.UnderTest
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.rdf.Aff4Schema
import com.github.nava2.aff4.model.rdf.MapStream
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

class MapIdxFileReaderTest {

  @get:Rule
  val rule: Aff4ImageTestRule = Aff4ImageTestRule("Base-Linear.aff4")

  @Inject
  @field:UnderTest
  private lateinit var aff4Model: Aff4Model

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var mapIdxFileReader: MapIdxFileReader

  private lateinit var mapStream: MapStream

  @Before
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
