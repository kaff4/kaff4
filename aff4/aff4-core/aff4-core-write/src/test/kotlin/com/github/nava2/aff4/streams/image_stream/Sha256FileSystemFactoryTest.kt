package com.github.nava2.aff4.streams.image_stream

import com.github.nava2.configuration.TestConfigProviderModule
import com.github.nava2.test.GuiceTestRule
import okio.ByteString.Companion.encodeUtf8
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import javax.inject.Inject

class Sha256FileSystemFactoryTest {
  private val tempDirectory = Files.createTempDirectory("test_").toOkioPath()

  @get:Rule
  val rule = GuiceTestRule(
    TestConfigProviderModule,
    WritingModule(tempDirectory),
  )

  @Inject
  private lateinit var sha256FileSystemFactory: Sha256FileSystemFactory

  private lateinit var shaTempDirectory: Path

  @Before
  fun setup() {
    shaTempDirectory = tempDirectory / Files.createTempDirectory(tempDirectory.toNioPath(), "sha1_").toString()
  }

  @After
  fun cleanup() {
    tempDirectory.toFile().deleteRecursively()
  }

  @Test
  fun `write file`() {
    val shaFileSystem = sha256FileSystemFactory.create(shaTempDirectory)
    val fooBarTxt = "foo".toPath() / "bar.txt"
    assertThat(shaFileSystem.exists(fooBarTxt)).isFalse()

    val fooBarTxtContent = "foobar\n".encodeUtf8()
    shaFileSystem.write(fooBarTxt, mustCreate = true) {
      write(fooBarTxtContent)
    }

    assertThat(shaFileSystem.exists(fooBarTxt)).isTrue()
    assertThat(shaFileSystem.read(fooBarTxt) { readByteString() }).isEqualTo(fooBarTxtContent)

    assertThat(shaFileSystem.list(fooBarTxt.parent!!)).containsExactly(fooBarTxt)
  }

  @Test
  fun `delete file`() {
    val shaFileSystem = sha256FileSystemFactory.create(shaTempDirectory)
    val fooBarTxt = "foo".toPath() / "bar.txt"

    shaFileSystem.write(fooBarTxt, mustCreate = true) { write("foobar\n".encodeUtf8()) }

    assertThat(shaFileSystem.exists(fooBarTxt)).isTrue()
    shaFileSystem.delete(fooBarTxt)
    assertThat(shaFileSystem.exists(fooBarTxt)).isFalse()
  }
}
