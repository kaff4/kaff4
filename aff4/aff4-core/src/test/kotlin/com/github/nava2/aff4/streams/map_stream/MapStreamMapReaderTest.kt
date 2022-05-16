package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.Aff4ImageTestRule
import com.github.nava2.aff4.meta.rdf.model.MapStream
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.streams.map_stream.tree.Interval
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

class MapStreamMapReaderTest {
  @get:Rule
  val rule: Aff4ImageTestRule = Aff4ImageTestRule()

  @Inject
  private lateinit var aff4Model: Aff4Model

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var mapStreamMapReader: MapStreamMapReader

  private lateinit var mapStream: MapStream

  @Before
  fun setup() {
    val mapStreamIri = valueFactory.createIRI("aff4://fcbfdce7-4488-4677-abf6-08bc931e195b")
    mapStream = aff4Model.get(mapStreamIri, MapStream::class)
  }

  @Test
  fun `load and query map`() {
    val mapMap = mapStreamMapReader.loadMap(mapStream)

    val queriedEntries = mapMap.query(0, mapStream.size).toList()
    assertThat(queriedEntries).hasSize(111)
  }

  @Test
  fun `query is always contiguous`() {
    val mapMap = mapStreamMapReader.loadMap(mapStream)

    assertContiguous(0, mapStream.size, mapMap.query(0, mapStream.size))
  }

  @Test
  fun `querying not at start and end truncates and fills in entries to be contiguous`() {
    val mapMap = mapStreamMapReader.loadMap(mapStream)

    assertContiguous(
      start = mapStream.size / 2,
      length = mapStream.size - 8 * 1024,
      entries = mapMap.query(mapStream.size / 2, mapStream.size - 8 * 1024),
    )
  }

  private fun assertContiguous(start: Long, length: Long, entries: Sequence<MapStreamEntry>) {
    var mergingInterval = Interval.Simple(start, 0)
    for (entry in entries) {
      assertThat(entry.mappedOffset).isEqualTo(mergingInterval.end)
      mergingInterval = mergingInterval.copy(length = entry.mappedEndOffset - mergingInterval.start)
    }

    assertThat(mergingInterval).isEqualTo(Interval.Simple(start, length))
  }
}
