package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.aff4.Aff4CoreModule
import com.github.nava2.aff4.UsingTemporary
import com.github.nava2.aff4.io.content
import com.github.nava2.aff4.io.md5
import com.github.nava2.aff4.io.repeatByteString
import com.github.nava2.aff4.model.rdf.CompressionMethod
import com.github.nava2.aff4.model.rdf.HashType
import com.github.nava2.aff4.model.rdf.ImageStream
import com.github.nava2.aff4.model.rdf.None
import com.github.nava2.aff4.model.rdf.hash
import com.github.nava2.aff4.model.rdf.toAff4Path
import com.github.nava2.aff4.rdf.MemoryRdfRepositoryPlugin
import com.github.nava2.aff4.streams.compression.SnappyCompression
import com.github.nava2.test.GuiceModule
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Timeout
import okio.buffer
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.ValueFactory
import org.junit.jupiter.api.Test
import javax.inject.Inject

class Aff4BevySinkTest {

  @GuiceModule
  val modules = listOf(
    Aff4CoreModule,
    MemoryRdfRepositoryPlugin,
  )

  @Inject
  private lateinit var valueFactory: ValueFactory

  @Inject
  private lateinit var bevyFactory: Bevy.Factory

  @UsingTemporary(useSha256 = true)
  private lateinit var imageFileSystem: FileSystem

  @Inject
  private lateinit var snappyCompression: SnappyCompression

  @Test
  fun `create bevy`() {
    val chunkSize = 10
    val chunksInSegment = 2
    val containerArn = valueFactory.createIRI("aff4://99cc4380-308f-4235-838c-e20a8898ad00")
    val imageStream = ImageStream(
      arn = valueFactory.createIRI("aff4://bb362b22-649c-494b-923f-e4ed0c5afef4"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = Long.MAX_VALUE,
      compressionMethod = CompressionMethod.None,
      stored = containerArn,
    )

    val blockHashes = listOf(HashType.SHA256, HashType.MD5)

    val bevy = bevyFactory.create(imageStream, 0, blockHashes)

    val content = "abcdefghijklmno".encodeUtf8()

    Aff4BevySink(
      outputFileSystem = imageFileSystem,
      timeout = Timeout.NONE,
      imageStream = imageStream,
      bevy = bevy,
    ).buffer().use { bevySink ->
      bevySink.write(content)
    }

    val imageStreamDir = imageStream.arn.toAff4Path(containerArn)

    val md5Content = content.hashChunks(HashType.MD5, chunkSize)
    val sha256Content = content.hashChunks(HashType.SHA256, chunkSize)

    assertThat(imageFileSystem).md5(imageStreamDir / "00000000", content.md5())
    assertThat(imageFileSystem).md5(imageStreamDir / "00000000.index", "cf7601068db0ad865e048ae071e1ecb2")
    assertThat(imageFileSystem).content(imageStreamDir / "00000000.md5", md5Content)
    assertThat(imageFileSystem).content(imageStreamDir / "00000000.sha256", sha256Content)
  }

  @Test
  fun `create snappy bevy`() {
    val content = 0.repeatByteString(100)

    val chunkSize = 50
    val chunksInSegment = 10
    val containerArn = valueFactory.createIRI("aff4://bb362b22-649c-494b-923f-e4ed0c5afef4")
    val imageStream = ImageStream(
      arn = valueFactory.createIRI("aff4://99cc4380-308f-4235-838c-e20a8898ad00"),
      chunkSize = chunkSize,
      chunksInSegment = chunksInSegment,
      size = content.size.toLong(),
      compressionMethod = snappyCompression,
      stored = containerArn,
    )

    val blockHashes = listOf(HashType.SHA256, HashType.MD5)

    val bevy = bevyFactory.create(imageStream, 0, blockHashes)

    Aff4BevySink(
      outputFileSystem = imageFileSystem,
      timeout = Timeout.NONE,
      imageStream = imageStream,
      bevy = bevy,
    ).buffer().use { bevySink ->
      bevySink.write(content)
    }

    val imageStreamDir = imageStream.arn.toAff4Path(containerArn)

    // All are manually verified to be correct, hashes changing implies we changed how we wrote the bevvy
    assertThat(imageFileSystem).md5(imageStreamDir / "00000000", "a324a567f37e730f66be9c1bf03d67ea")
    assertThat(imageFileSystem).md5(imageStreamDir / "00000000.index", "1ad20d3f7dcc686c7ecb83d4c8f8bd07")
    assertThat(imageFileSystem).md5(imageStreamDir / "00000000.md5", "79d453554ccbb3384217e54ac0924d79")
    assertThat(imageFileSystem).md5(imageStreamDir / "00000000.sha256", "a9e68f6c25975e6b6f650166d2f030f1")
  }
}

internal fun ByteString.hashChunks(
  hashType: HashType,
  chunkSize: Int,
): ByteString = okio.Buffer().use { contentBuffer ->
  val contentChunks = toByteArray().asSequence().chunked(chunkSize) {
    it.toByteArray().toByteString()
  }

  for (chunk in contentChunks) {
    contentBuffer.write(chunk.hash(hashType))
  }

  contentBuffer.readByteString()
}
