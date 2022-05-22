package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.Aff4ImageTestRule
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.model.VerifiableStream
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.MapStream
import com.github.nava2.aff4.streams.Hashing.hashingSink
import com.github.nava2.aff4.streams.compression.SnappyModule
import com.github.nava2.aff4.streams.md5
import com.github.nava2.aff4.streams.repeatByteString
import okio.Buffer
import okio.ByteString.Companion.decodeHex
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
  val rule: Aff4ImageTestRule = Aff4ImageTestRule("Base-Linear.aff4", SnappyModule)

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
    aff4MapStream.close()
  }

  @Test
  fun `open and read map`() {
    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
      assertThat(mapStreamSource).md5(CHUNK_SIZE, 0.repeatByteString(CHUNK_SIZE.toInt()).md5())
    }
  }

  @Test
  fun `open and read multiple times has same read`() {
    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
    }

    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
    }
  }

  @Test
  fun `open and read gt chunk size`() {
    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE * 100, "036b865ad3b624bf29ed27e53d3e86ee")
    }
  }

  @Test
  fun `creating sources at location effectively seeks the stream`() {
    createSource(position = CHUNK_SIZE).use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, 0.repeatByteString(CHUNK_SIZE.toInt()).md5())
    }

    createSource(position = 0).use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
    }
  }

  @Test
  fun `open and read skip bytes via buffering`() {
    createSource().use { mapStreamSource ->
      mapStreamSource.skip(1024)
      assertThat(mapStreamSource).md5(CHUNK_SIZE - 1024, "50615dd05bb46aafc9490a7c48391314")
    }

    createSource().use { mapStreamSource ->
      mapStreamSource.skip(1024)
      assertThat(mapStreamSource).md5(CHUNK_SIZE, 0.repeatByteString(CHUNK_SIZE.toInt()).md5())
    }
  }

  @Test
  fun `reading past end truncates`() {
    createSource(mapStream.size - 2048).use { mapStreamSource ->
      Buffer().use { readSink ->
        assertThat(mapStreamSource.read(readSink, 4096)).isEqualTo(2048)
        assertThat(readSink.size).isEqualTo(2048)
        assertThat(readSink.md5()).isEqualTo(0.repeatByteString(2048).md5())
      }
    }
  }

  @Test
  fun `hashes match`() {
    assertThat(aff4MapStream.verify(aff4Model)).isEqualTo(VerifiableStream.Result.Success)

    createSource().use { source ->
      val md5Sink = HashType.MD5.hashingSink()
      val sha1Sink = HashType.SHA1.hashingSink(md5Sink)
      source.readAll(sha1Sink)
      assertThat(sha1Sink.hash).isEqualTo("7d3d27f667f95f7ec5b9d32121622c0f4b60b48d".decodeHex())
      assertThat(md5Sink.hash).isEqualTo("dd6dbda282e27fd0d196abd95f5c3e58".decodeHex())
    }
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
