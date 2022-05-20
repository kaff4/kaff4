package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.Aff4ImageTestRule
import com.github.nava2.aff4.meta.rdf.model.MapStream
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.streams.VerifiableStream
import com.github.nava2.aff4.streams.compression.SnappyModule
import com.github.nava2.aff4.streams.md5
import com.github.nava2.aff4.streams.repeatByteString
import okio.Buffer
import okio.buffer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

private const val CHUNK_SIZE: Long = 32 * 1024

class Aff4MapStreamTest {
  @get:Rule
  val rule: Aff4ImageTestRule = Aff4ImageTestRule(SnappyModule)

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var aff4StreamOpener: Aff4StreamOpener

  @Inject
  private lateinit var aff4Model: Aff4Model

  private lateinit var aff4MapStream: Aff4MapStream
  private lateinit var mapStream: MapStream

  @Before
  fun setup() {
    val mapStreamIri = valueFactory.createIRI("aff4://fcbfdce7-4488-4677-abf6-08bc931e195b")
    aff4MapStream = aff4StreamOpener.openStream(mapStreamIri) as Aff4MapStream
    mapStream = aff4MapStream.mapStream
  }

  @After
  fun close() {
    aff4StreamOpener.close()
    aff4MapStream.close()
  }

  @Test
  fun `open and read map`() {
    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "1f11e90ee7959d2da6cb0b6067ba1a05")
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "bb7df04e1b0a2570657527a7e108ae23")
    }
  }

  @Test
  fun `open and read multiple times has same read`() {
    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "1f11e90ee7959d2da6cb0b6067ba1a05")
    }

    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "1f11e90ee7959d2da6cb0b6067ba1a05")
    }
  }

  @Test
  fun `open and read gt chunk size`() {
    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE * 100, "f047e55361bd52d073df651b7acf4509")
    }
  }

  @Test
  fun `creating sources at location effectively seeks the stream`() {
    createSource(position = CHUNK_SIZE * 12).use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "bb7df04e1b0a2570657527a7e108ae23")
    }

    createSource(position = 0).use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "1f11e90ee7959d2da6cb0b6067ba1a05")
    }
  }

  @Test
  fun `open and read skip bytes via buffering`() {
    createSource().use { mapStreamSource ->
      mapStreamSource.skip(1024)
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "018634489419068058d745ea1645705d")
    }
  }

  @Test
  fun `reading past end truncates`() {
    createSource(mapStream.size - 2048).use { mapStreamSource ->
      Buffer().use { readSink ->
        assertThat(mapStreamSource.readAll(readSink)).isEqualTo(2048)
        assertThat(readSink.size).isEqualTo(2048)
        assertThat(readSink.md5()).isEqualTo(0.repeatByteString(2048).md5())
      }
    }
  }

  @Test
  fun `hashes match`() {
    assertThat(aff4MapStream.verify(aff4Model)).isEqualTo(VerifiableStream.Result.Success)
  }

  @Test
  fun `having open sources causes close() to throw`() {
    createSource().use { source ->
      assertThatThrownBy { aff4MapStream.close() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Sources were created and not freed: 1")

      source.close()
      aff4MapStream.close() // no throw
    }
  }

  private fun createSource(position: Long = 0) = aff4MapStream.source(position).buffer()
}
