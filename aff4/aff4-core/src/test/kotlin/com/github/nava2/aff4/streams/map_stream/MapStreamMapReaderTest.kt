package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.Aff4ImageTestRule
import com.github.nava2.aff4.interval_tree.Interval
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.rdf.MapStream
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.function.Consumer
import javax.inject.Inject

class MapStreamMapReaderTest {
  @get:Rule
  val rule: Aff4ImageTestRule = Aff4ImageTestRule("Base-Linear.aff4")

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
    assertThat(queriedEntries).hasSize(20)
  }

  @Test
  fun `query is always contiguous`() {
    val mapMap = mapStreamMapReader.loadMap(mapStream)

    assertThat(mapMap).isContiguousQuery(start = 0, length = mapStream.size)
  }

  @Test
  fun `query truncates to max size`() {
    val mapMap = mapStreamMapReader.loadMap(mapStream)

    val offset = mapStream.size - 1024
    assertThat(mapMap.query(offset, 4 * 1024)).isContiguous(offset, 1024)
  }

  @Test
  fun `querying not at start and end fills in entries to be contiguous to next valid entry`() {
    val mapMap = mapStreamMapReader.loadMap(mapStream)

    val start = mapStream.size / 2
    val length = mapStream.size / 2 - 8 * 1024
    val extendedLength = 134_209_536L
    assertThat(mapMap.query(start, length)).isContiguous(start, extendedLength)
  }

  private fun ObjectAssert<MapStreamMap>.isContiguousQuery(start: Long, length: Long): ObjectAssert<MapStreamMap> {
    return satisfies(
      Consumer { mapMap ->
        assertThat(mapMap.query(start, length)).isContiguous(start, length)
      }
    )
  }

  private fun ObjectAssert<Sequence<MapStreamEntry>>.isContiguous(
    start: Long,
    length: Long,
  ): ObjectAssert<Sequence<MapStreamEntry>> {
    return satisfies(
      Consumer { results ->
        var mergingInterval = Interval.Simple(start, 0)

        assertThat(results.asIterable()).allSatisfy(
          Consumer { entry ->
            assertThat(entry.mappedOffset).isEqualTo(mergingInterval.end)
            mergingInterval = mergingInterval.copy(length = entry.mappedEndOffset - mergingInterval.start)
          }
        )

        assertThat(mergingInterval).isEqualTo(Interval.Simple(start, length))
      }
    )
  }
}
