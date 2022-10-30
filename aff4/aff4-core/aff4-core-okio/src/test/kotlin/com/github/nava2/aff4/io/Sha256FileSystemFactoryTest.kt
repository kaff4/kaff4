package com.github.nava2.aff4.io

import com.github.nava2.test.GuiceExtension
import okio.ByteString.Companion.encodeUtf8
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import javax.inject.Inject

@ExtendWith(GuiceExtension::class)
class Sha256FileSystemFactoryTest {
  @TempDir
  private lateinit var tempDirectory: Path

  @Inject
  private lateinit var sha256FileSystemFactory: Sha256FileSystemFactory

  @Test
  fun `write file`() {
    val shaFileSystem = sha256FileSystemFactory.create(tempDirectory)
    val fooBarTxt = "foo".toPath() / "bar.txt"
    assertThat(shaFileSystem).doesNotExist(fooBarTxt)

    val fooBarTxtContent = "foobar\n".encodeUtf8()
    shaFileSystem.write(fooBarTxt, mustCreate = true) {
      write(fooBarTxtContent)
    }

    assertThat(shaFileSystem).exists(fooBarTxt)
    assertThat(shaFileSystem).content(fooBarTxt, fooBarTxtContent)

    assertThat(shaFileSystem.mappingsView).containsEntry(
      fooBarTxt,
      "ff".toPath() / "ff4111e8e4ebd83e0173a4f765a6d9e71068b5732f1e0c3ba865dfe1b8c6fad2"
    )

    assertThat(shaFileSystem.list(fooBarTxt.parent!!)).containsExactly(fooBarTxt)
  }

  @Test
  fun `write multiple files file`() {
    val shaFileSystem = sha256FileSystemFactory.create(tempDirectory)
    val fooBarTxt = "foo".toPath() / "bar.txt"
    val fooBazTxt = "foo".toPath() / "baz.txt"

    assertThat(shaFileSystem).doesNotExist(fooBarTxt)
    assertThat(shaFileSystem).doesNotExist(fooBazTxt)

    val fooBarTxtContent = "foobar\n".encodeUtf8()
    shaFileSystem.write(fooBarTxt, mustCreate = true) {
      write(fooBarTxtContent)
    }

    val fooBazTxtContent = "foobaz\n".encodeUtf8()
    shaFileSystem.write(fooBazTxt, mustCreate = true) {
      write(fooBazTxtContent)
    }

    assertThat(shaFileSystem).exists(fooBarTxt)
    assertThat(shaFileSystem).exists(fooBazTxt)
    assertThat(shaFileSystem).content(fooBarTxt, fooBarTxtContent)
    assertThat(shaFileSystem).content(fooBazTxt, fooBazTxtContent)

    assertThat(shaFileSystem.mappingsView)
      .containsEntry(
        fooBarTxt,
        "ff".toPath() / "ff4111e8e4ebd83e0173a4f765a6d9e71068b5732f1e0c3ba865dfe1b8c6fad2"
      )
      .containsEntry(
        fooBazTxt,
        "ef".toPath() / "effa498b575546a19dc5242a525d665f14cbdb2e9be16561786e5c9937273a8f"
      )
  }

  @Test
  fun `directory operations`() {
    val shaFileSystem = sha256FileSystemFactory.create(tempDirectory)
    val fooBarTxt = "foo".toPath() / "bar.txt"
    val fooBazTxt = "foo".toPath() / "baz.txt"

    val fooBarTxtContent = "foobar\n".encodeUtf8()
    shaFileSystem.write(fooBarTxt, mustCreate = true) {
      write(fooBarTxtContent)
    }

    val fooBazTxtContent = "foobaz\n".encodeUtf8()
    shaFileSystem.write(fooBazTxt, mustCreate = true) {
      write(fooBazTxtContent)
    }

    assertThat(shaFileSystem).exists(fooBarTxt)
    assertThat(shaFileSystem).exists(fooBazTxt)

    assertThat(shaFileSystem.list(fooBarTxt.parent!!)).containsExactlyInAnyOrder(fooBarTxt, fooBazTxt)

    assertThat(shaFileSystem.listRecursively("".toPath()).toList()).containsExactlyInAnyOrder(
      "foo".toPath(),
      fooBarTxt,
      fooBazTxt,
    )

    assertThat(shaFileSystem.metadataOrNull(fooBarTxt)).isRegularFile(fooBarTxtContent.size.toLong())
    assertThat(shaFileSystem.metadataOrNull(fooBazTxt)).isRegularFile(fooBazTxtContent.size.toLong())

    assertThat(shaFileSystem).exists("foo".toPath())
    assertThat(shaFileSystem.metadata("foo".toPath())).isDirectory()
    assertThat(shaFileSystem.metadataOrNull("foo".toPath())).isDirectory()

    assertThat(shaFileSystem).doesNotExist("does_not_exist".toPath())
    assertThat(shaFileSystem.metadataOrNull("does_not_exist/foo".toPath()))
      .isNull()
  }

  @Test
  fun `delete file`() {
    val shaFileSystem = sha256FileSystemFactory.create(tempDirectory)
    val fooBarTxt = "foo".toPath() / "bar.txt"

    shaFileSystem.write(fooBarTxt, mustCreate = true) { write("foobar\n".encodeUtf8()) }

    assertThat(shaFileSystem).exists(fooBarTxt)

    assertThat(shaFileSystem.mappingsView).containsEntry(
      fooBarTxt,
      "ff".toPath() / "ff4111e8e4ebd83e0173a4f765a6d9e71068b5732f1e0c3ba865dfe1b8c6fad2"
    )

    shaFileSystem.delete(fooBarTxt)

    assertThat(shaFileSystem).doesNotExist(fooBarTxt)

    // BUG At one point, this returned true the second time due to map shenanigans
    assertThat(shaFileSystem).doesNotExist(fooBarTxt)
  }
}
