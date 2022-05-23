package com.github.nava2.aff4.io

import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
import okio.Path
import okio.buffer
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import java.util.function.Consumer

fun <SELF : AbstractObjectAssert<SELF, T>, T : FileSystem> SELF.md5(path: Path, md5: String): SELF {
  return md5(path, md5.decodeHex())
}

fun <SELF : AbstractObjectAssert<SELF, T>, T : FileSystem> SELF.md5(path: Path, md5: ByteString): SELF {
  exists(path)

  return satisfies(
    Consumer { fs ->
      val fileSize = fs.metadata(path).size!!
      fs.source(path).use { source ->
        assertThat(source)
          .`as` { "md5(fileSystem.source($path)) [fileSystem=$fs]" }
          .md5(fileSize, md5)
      }
    }
  )
}

fun <SELF : AbstractObjectAssert<SELF, T>, T : FileSystem> SELF.exists(path: Path): SELF {
  return satisfies(
    Consumer { fs ->
      assertThat(fs.exists(path)).`as` { "fileSystem.exists($path) [fileSystem=$fs]" }.isTrue()
    }
  )
}

fun <SELF : AbstractObjectAssert<SELF, T>, T : FileSystem> SELF.doesNotExist(path: Path): SELF {
  return satisfies(
    Consumer { fs ->
      assertThat(fs.exists(path)).`as` { "!fileSystem.exists($path) [fileSystem=$fs]" }.isFalse()
    }
  )
}

fun <SELF : AbstractObjectAssert<SELF, T>, T : FileSystem> SELF.content(path: Path, content: ByteString): SELF {
  exists(path)

  return satisfies(
    Consumer { fs ->
      fs.source(path).buffer().use { source ->
        assertThat(source.readByteString())
          .`as` { "fileSystem.source($path) [fileSystem=$fs]" }
          .isEqualTo(content)
      }
    }
  )
}
