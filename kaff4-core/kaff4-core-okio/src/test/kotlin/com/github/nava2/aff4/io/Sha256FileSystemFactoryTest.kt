package com.github.nava2.aff4.io

import com.github.nava2.aff4.UsingTemporary
import com.github.nava2.aff4.satisfies
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path.Companion.toPath
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class Sha256FileSystemFactoryTest {
  @UsingTemporary
  private lateinit var tempFileSystem: FileSystem

  private val sha256FileSystemFactory = Sha256FileSystemFactory()

  private lateinit var shaFileSystem: Sha256FileSystemFactory.MappedFileSystem

  @BeforeEach
  fun setup() {
    shaFileSystem = sha256FileSystemFactory.create(tempFileSystem, ".".toPath())
  }

  @Test
  fun `verify mapping uses sha256 with two char prefix`() {
    // *NIX systems will use /
    assertThat(shaFileSystem.mapExternalPath("foo/bar.txt".toPath()))
      .isEqualTo("ff".toPath() / "ff4111e8e4ebd83e0173a4f765a6d9e71068b5732f1e0c3ba865dfe1b8c6fad2")

    // dos use \\
    assertThat(shaFileSystem.mapExternalPath("foo\\bar.txt".toPath()))
      .isEqualTo("55".toPath() / "5509116d245044864c22a8c6aed0cd5139ad8a2851f86c386f2b6aa5e1496917")
  }

  @Test
  fun `write file`() {
    val fooBarTxt = "foo".toPath() / "bar.txt"
    assertThat(shaFileSystem).doesNotExist(fooBarTxt)

    val fooBarTxtContent = "foobar\n".encodeUtf8()
    shaFileSystem.write(fooBarTxt, mustCreate = true) {
      write(fooBarTxtContent)
    }

    assertThat(shaFileSystem).exists(fooBarTxt)
    assertThat(shaFileSystem).content(fooBarTxt, fooBarTxtContent)

    assertThat(shaFileSystem).hasMappings(fooBarTxt)

    assertThat(shaFileSystem.list(fooBarTxt.parent!!)).containsExactly(fooBarTxt)
  }

  @Test
  fun `write multiple files file`() {
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

    assertThat(shaFileSystem).hasMappings(fooBarTxt, fooBazTxt)
  }

  @Test
  fun `directory operations`() {
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

    assertThat(shaFileSystem.listRecursively("".toPath()).toList())
      .containsExactlyInAnyOrder(
        "foo".toPath(),
        fooBarTxt,
        fooBazTxt,
      )

    assertThat(shaFileSystem).hasMappings(
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
    val fooBarTxt = "foo".toPath() / "bar.txt"

    shaFileSystem.write(fooBarTxt, mustCreate = true) { write("foobar\n".encodeUtf8()) }

    assertThat(shaFileSystem).exists(fooBarTxt)

    assertThat(shaFileSystem).hasMappings(fooBarTxt)

    shaFileSystem.delete(fooBarTxt)

    assertThat(shaFileSystem).doesNotExist(fooBarTxt)

    // BUG At one point, this returned true the second time due to map shenanigans
    assertThat(shaFileSystem).doesNotExist(fooBarTxt)
  }
}

private fun <SELF : AbstractObjectAssert<SELF, Sha256FileSystemFactory.MappedFileSystem>> SELF.hasMappings(
  vararg paths: okio.Path,
): SELF {
  return satisfies { fs ->
    assertThat(fs.mappingsView)
      .containsAllEntriesOf(paths.associateWith { fs.mapExternalPath(it) })
  }
}
