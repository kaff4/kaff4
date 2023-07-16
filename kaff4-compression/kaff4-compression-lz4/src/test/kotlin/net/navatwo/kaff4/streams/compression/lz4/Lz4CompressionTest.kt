package net.navatwo.kaff4.streams.compression.lz4

import jakarta.inject.Inject
import net.navatwo.kaff4.streams.compression.BaseCompressionMethodTest
import net.navatwo.test.GuiceModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class Lz4CompressionTest : BaseCompressionMethodTest() {
  @GuiceModule
  val testModule = Aff4Lz4FeatureModule

  @Inject
  override lateinit var compressionMethod: Lz4Compression

  override val compressedDataValues = CompressedDataValues(
    compressedSize = 77,
    md5 = "f0cad3b9abc1ed712b0d616fc8e87381",
  )

  @Test
  fun `method == IDENTIFIER`() {
    assertThat(compressionMethod.method).isEqualTo(Lz4Compression.IDENTIFIER)
  }
}
