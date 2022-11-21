package com.github.nava2.aff4.streams.zip_segment

import com.github.nava2.aff4.Aff4ImageTestModule
import com.github.nava2.aff4.Aff4LogicalModule
import com.github.nava2.aff4.UnderTest
import com.github.nava2.aff4.io.buffer
import com.github.nava2.aff4.io.md5
import com.github.nava2.aff4.io.use
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.model.VerifiableStreamProvider
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.aff4.streams.hashingSink
import com.github.nava2.test.GuiceModule
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject

private const val DREAM_TXT_SIZE = 8688L
private val DREAM_FIRST_LINE = "I have a Dream by Martin Luther King, Jr; August 28, 1963\n".encodeUtf8()
private val DREAM_LAST_STANZA =
  "\"Free at last! free at last! Thank God Almighty, we are free at last!\"\n".encodeUtf8()

class Aff4ZipSegmentTest {
  @GuiceModule
  val imageTestModules = listOf(
    Aff4ImageTestModule(imageName = "dream.aff4"),
    Aff4LogicalModule,
  )

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  @field:UnderTest
  private lateinit var aff4StreamOpener: Aff4StreamOpener

  @Inject
  @field:UnderTest
  private lateinit var aff4Model: Aff4Model

  private lateinit var aff4ZipSegment: Aff4ZipSegmentSourceProvider
  private lateinit var zipSegment: ZipSegment
  private val bufferedProvider by lazy { aff4ZipSegment.buffer() }

  @BeforeEach
  fun setup() {
    val zipSegmentIri =
      valueFactory.createIRI("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83//test_images/AFF4-L/dream.txt")
    aff4ZipSegment = aff4StreamOpener.openStream(zipSegmentIri) as Aff4ZipSegmentSourceProvider
    zipSegment = aff4ZipSegment.zipSegment
  }

  @Test
  fun `open and read map`() {
    assertThat(aff4ZipSegment.size).isEqualTo(DREAM_TXT_SIZE)
    assertThat(zipSegment.size).isEqualTo(DREAM_TXT_SIZE)

    bufferedProvider.use { mapStreamSource ->
      assertThat(mapStreamSource).md5(DREAM_TXT_SIZE, "75d83773f8d431a3ca91bfb8859e486d")
    }
  }

  @Test
  fun `open and read multiple times has same read`() {
    bufferedProvider.use { mapStreamSource ->
      assertThat(mapStreamSource.readByteString(DREAM_FIRST_LINE.size.toLong())).isEqualTo(DREAM_FIRST_LINE)
      mapStreamSource.skip(DREAM_TXT_SIZE - DREAM_LAST_STANZA.size - DREAM_FIRST_LINE.size)
      assertThat(mapStreamSource.readByteString()).isEqualTo(DREAM_LAST_STANZA)
    }

    bufferedProvider.use { mapStreamSource ->
      assertThat(mapStreamSource.readByteString(DREAM_FIRST_LINE.size.toLong())).isEqualTo(DREAM_FIRST_LINE)
    }
  }

  @Test
  fun `creating sources at location effectively seeks the stream`() {
    bufferedProvider.use(position = DREAM_TXT_SIZE - DREAM_LAST_STANZA.size) { mapStreamSource ->
      assertThat(mapStreamSource.readByteString()).isEqualTo(DREAM_LAST_STANZA)
    }

    bufferedProvider.use(position = 0) { mapStreamSource ->
      assertThat(mapStreamSource).md5(DREAM_TXT_SIZE, "75d83773f8d431a3ca91bfb8859e486d")
    }
  }

  @Test
  fun `reading past end truncates`() {
    bufferedProvider.use(position = DREAM_TXT_SIZE - DREAM_LAST_STANZA.size) { mapStreamSource ->
      assertThat(mapStreamSource.readByteString()).isEqualTo(DREAM_LAST_STANZA)
    }
  }

  @Test
  fun `hashes match`() {
    assertThat(aff4ZipSegment.verify(aff4Model)).isEqualTo(VerifiableStreamProvider.Result.Success)

    bufferedProvider.use { source ->
      val md5Sink = HashType.MD5.hashingSink()
      val sha1Sink = HashType.SHA1.hashingSink(md5Sink)
      source.readAll(sha1Sink)
      assertThat(sha1Sink.hash).isEqualTo("9ae1b46bead70c322eef7ac8bc36a8ea2055595c".decodeHex())
      assertThat(md5Sink.hash).isEqualTo("75d83773f8d431a3ca91bfb8859e486d".decodeHex())
    }
  }
}
