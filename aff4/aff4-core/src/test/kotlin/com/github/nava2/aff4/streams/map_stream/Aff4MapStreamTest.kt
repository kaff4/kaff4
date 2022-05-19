package com.github.nava2.aff4.streams.map_stream

import com.github.nava2.aff4.Aff4ImageTestRule
import com.github.nava2.aff4.meta.rdf.model.MapStream
import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.streams.compression.SnappyModule
import com.github.nava2.aff4.streams.md5
import com.github.nava2.aff4.streams.repeatByteString
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.After
import org.junit.Before
import org.junit.Ignore
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
  @Ignore
  fun `open and read bevy source`() {
    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "86a8ec10b992e4b9236eb4eadca432d5")
    }
  }

  @Test
  @Ignore
  fun `open and read multiple times has same read`() {
    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
    }

    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
    }
  }

  @Test
  @Ignore
  fun `open and read gt chunk size`() {
    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE * 100, "866f93925759a39af236632470789234")
    }
  }

  @Test
  @Ignore
  fun `creating sources at location effectively seeks the stream`() {
    createSource().use { s ->
      FileSystem.SYSTEM.appendingSink("./dumped.bin".toPath()).buffer().use {
        s.readAll(it)
      }
    }

    createSource(position = CHUNK_SIZE * 12).use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "86a8ec10b992e4b9236eb4eadca432d5")
    }

    createSource(position = 0).use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
    }
  }

  @Test
  @Ignore
  fun `open and read skip bytes via buffering`() {
    createSource().use { mapStreamSource ->
      mapStreamSource.skip(1024)
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "fea53f346a83f6fca5d4fa89ac96e758")
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

//  @Test
//  fun `hashes match`() {
//    createSource().use { mapStreamSource ->
//      Buffer().use { readSink ->
//        assertThat(mapStreamSource.readAll(readSink)).isEqualTo(mapStream.size)
//        assertThat(readSink.md5()).isEqualTo(mapStream.linearHashes.single { it is Hash.Md5 }.hash)
//        assertThat(readSink.sha1()).isEqualTo(mapStream.linearHashes.single { it is Hash.Sha1 }.hash)
//      }
//    }
//
//    Assertions.assertThat(aff4ImageStream.verify()).isEqualTo(VerifiableStream.Result.Success)
//  }

  @Test
  fun `having open sources causes close() to throw`() {
    createSource().use { source ->
      Assertions.assertThatThrownBy { aff4MapStream.close() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Sources were created and not freed: 1")

      source.close()
      aff4MapStream.close() // no throw
    }
  }

  private fun createSource(position: Long = 0) = aff4MapStream.source(position).buffer()
}
