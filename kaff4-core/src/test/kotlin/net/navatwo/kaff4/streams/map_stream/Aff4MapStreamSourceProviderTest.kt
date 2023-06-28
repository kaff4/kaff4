package net.navatwo.kaff4.streams.map_stream

import jakarta.inject.Inject
import net.navatwo.kaff4.Aff4ImageTestModule
import net.navatwo.kaff4.BaseLinear
import net.navatwo.kaff4.UnderTest
import net.navatwo.kaff4.io.buffer
import net.navatwo.kaff4.io.md5
import net.navatwo.kaff4.io.repeatByteString
import net.navatwo.kaff4.io.use
import net.navatwo.kaff4.model.Aff4Model
import net.navatwo.kaff4.model.Aff4StreamOpener
import net.navatwo.kaff4.model.VerifiableStreamProvider
import net.navatwo.kaff4.model.rdf.HashType
import net.navatwo.kaff4.model.rdf.MapStream
import net.navatwo.kaff4.streams.hashingSink
import net.navatwo.test.GuiceModule
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val CHUNK_SIZE: Long = 32 * 1024

class Aff4MapStreamSourceProviderTest {
  @GuiceModule
  val module = Aff4ImageTestModule.BaseLinear

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  @field:UnderTest
  private lateinit var aff4StreamOpener: Aff4StreamOpener

  @Inject
  @field:UnderTest
  private lateinit var aff4Model: Aff4Model

  private lateinit var aff4MapStreamSourceProvider: Aff4MapStreamSourceProvider
  private lateinit var mapStream: MapStream

  private val bufferedProvider by lazy { aff4MapStreamSourceProvider.buffer() }

  @BeforeEach
  fun setup() {
    val mapStreamIri = valueFactory.createIRI("aff4://fcbfdce7-4488-4677-abf6-08bc931e195b")
    aff4MapStreamSourceProvider = aff4StreamOpener.openStream(mapStreamIri) as Aff4MapStreamSourceProvider
    mapStream = aff4MapStreamSourceProvider.mapStream
  }

  @Test
  fun `open and read map`() {
    bufferedProvider.use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
      assertThat(mapStreamSource).md5(CHUNK_SIZE, 0.repeatByteString(CHUNK_SIZE.toInt()).md5())
    }
  }

  @Test
  fun `open and read multiple times has same read`() {
    bufferedProvider.use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
    }

    bufferedProvider.use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
    }
  }

  @Test
  fun `open and read gt chunk size`() {
    bufferedProvider.use { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE * 100, "036b865ad3b624bf29ed27e53d3e86ee")
    }
  }

  @Test
  fun `creating sources at location effectively seeks the stream`() {
    bufferedProvider.use(position = CHUNK_SIZE) { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, 0.repeatByteString(CHUNK_SIZE.toInt()).md5())
    }

    bufferedProvider.use(position = 0) { mapStreamSource ->
      assertThat(mapStreamSource).md5(CHUNK_SIZE, "af05fdbda3150e658948ba8b74f1fe82")
    }
  }

  @Test
  fun `open and read skip bytes via buffering`() {
    bufferedProvider.use { mapStreamSource ->
      mapStreamSource.skip(1024)
      assertThat(mapStreamSource).md5(CHUNK_SIZE - 1024, "50615dd05bb46aafc9490a7c48391314")
    }

    bufferedProvider.use { mapStreamSource ->
      mapStreamSource.skip(1024)
      assertThat(mapStreamSource).md5(CHUNK_SIZE, 0.repeatByteString(CHUNK_SIZE.toInt()).md5())
    }
  }

  @Test
  fun `reading past end truncates`() {
    bufferedProvider.use(mapStream.size - 2048) { mapStreamSource ->
      Buffer().use { readSink ->
        assertThat(mapStreamSource.read(readSink, 4096)).isEqualTo(2048)
        assertThat(readSink.size).isEqualTo(2048)
        assertThat(readSink.md5()).isEqualTo(0.repeatByteString(2048).md5())
      }
    }
  }

  @Test
  fun `hashes match`() {
    assertThat(aff4MapStreamSourceProvider.verify(aff4Model)).isEqualTo(VerifiableStreamProvider.Result.Success)

    bufferedProvider.use { source ->
      HashType.MD5.hashingSink().use { md5Sink ->
        HashType.SHA1.hashingSink(md5Sink).use { sha1Sink ->
          source.readAll(sha1Sink)
          assertThat(sha1Sink.hash).isEqualTo("7d3d27f667f95f7ec5b9d32121622c0f4b60b48d".decodeHex())
        }

        assertThat(md5Sink.hash).isEqualTo("dd6dbda282e27fd0d196abd95f5c3e58".decodeHex())
      }
    }
  }
}
