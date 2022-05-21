package com.github.nava2.aff4.streams.zip_segment

import com.github.nava2.aff4.Aff4LogicalImageTestRule
import com.github.nava2.aff4.model.Aff4Model
import com.github.nava2.aff4.model.Aff4StreamOpener
import com.github.nava2.aff4.model.VerifiableStream
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ZipSegment
import com.github.nava2.aff4.streams.Hashing.hashingSink
import com.github.nava2.aff4.streams.md5
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.buffer
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

private const val DREAM_TXT_SIZE = 8688L
private val DREAM_FIRST_LINE = "I have a Dream by Martin Luther King, Jr; August 28, 1963\n".encodeUtf8()
private val DREAM_LAST_STANZA =
  "\"Free at last! free at last! Thank God Almighty, we are free at last!\"\n".encodeUtf8()

class Aff4ZipSegmentTest {
  @get:Rule
  val rule = Aff4LogicalImageTestRule()

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var aff4StreamOpener: Aff4StreamOpener

  @Inject
  private lateinit var aff4Model: Aff4Model

  private lateinit var aff4ZipSegment: Aff4ZipSegment
  private lateinit var zipSegment: ZipSegment

  @Before
  fun setup() {
    val zipSegmentIri =
      valueFactory.createIRI("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83//test_images/AFF4-L/dream.txt")
    aff4ZipSegment = aff4StreamOpener.openStream(zipSegmentIri) as Aff4ZipSegment
    zipSegment = aff4ZipSegment.zipSegment
  }

  @After
  fun close() {
    aff4ZipSegment.close()
  }

  @Test
  fun `open and read map`() {
    assertThat(aff4ZipSegment.size).isEqualTo(DREAM_TXT_SIZE)
    assertThat(zipSegment.size).isEqualTo(DREAM_TXT_SIZE)

    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource).md5(DREAM_TXT_SIZE, "75d83773f8d431a3ca91bfb8859e486d")
    }
  }

  @Test
  fun `open and read multiple times has same read`() {
    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource.readByteString(DREAM_FIRST_LINE.size.toLong())).isEqualTo(DREAM_FIRST_LINE)
      mapStreamSource.skip(DREAM_TXT_SIZE - DREAM_LAST_STANZA.size - DREAM_FIRST_LINE.size)
      assertThat(mapStreamSource.readByteString()).isEqualTo(DREAM_LAST_STANZA)
    }

    createSource().use { mapStreamSource ->
      assertThat(mapStreamSource.readByteString(DREAM_FIRST_LINE.size.toLong())).isEqualTo(DREAM_FIRST_LINE)
    }
  }

  @Test
  fun `creating sources at location effectively seeks the stream`() {
    createSource(position = DREAM_TXT_SIZE - DREAM_LAST_STANZA.size).use { mapStreamSource ->
      assertThat(mapStreamSource.readByteString()).isEqualTo(DREAM_LAST_STANZA)
    }

    createSource(position = 0).use { mapStreamSource ->
      assertThat(mapStreamSource).md5(DREAM_TXT_SIZE, "75d83773f8d431a3ca91bfb8859e486d")
    }
  }

  @Test
  fun `reading past end truncates`() {
    createSource(DREAM_TXT_SIZE - DREAM_LAST_STANZA.size).use { mapStreamSource ->
      assertThat(mapStreamSource.readByteString()).isEqualTo(DREAM_LAST_STANZA)
    }
  }

  @Test
  fun `hashes match`() {
    assertThat(aff4ZipSegment.verify(aff4Model)).isEqualTo(VerifiableStream.Result.Success)

    createSource().use { source ->
      val md5Sink = HashType.MD5.hashingSink()
      val sha1Sink = HashType.SHA1.hashingSink(md5Sink)
      source.readAll(sha1Sink)
      assertThat(sha1Sink.hash).isEqualTo("9ae1b46bead70c322eef7ac8bc36a8ea2055595c".decodeHex())
      assertThat(md5Sink.hash).isEqualTo("75d83773f8d431a3ca91bfb8859e486d".decodeHex())
    }
  }

  @Test
  fun `having open sources causes close() to throw`() {
    createSource().use { source ->
      Assertions.assertThatThrownBy { aff4ZipSegment.close() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Sources were created and not freed: 1")

      source.close()
      aff4ZipSegment.close() // no throw
    }
  }

  private fun createSource(position: Long = 0) = aff4ZipSegment.source(position).buffer()
}
