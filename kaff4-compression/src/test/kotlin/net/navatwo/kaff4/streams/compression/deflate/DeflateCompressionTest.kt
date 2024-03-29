package net.navatwo.kaff4.streams.compression.deflate

import jakarta.inject.Inject
import net.navatwo.kaff4.streams.compression.BaseCompressionMethodTest
import net.navatwo.test.GuiceModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DeflateCompressionTest : BaseCompressionMethodTest() {
  @GuiceModule
  val testModule = Aff4DeflateFeatureModule

  @Inject
  override lateinit var compressionMethod: DeflateCompression

  override val compressedDataValues = CompressedDataValues(
    compressedSize = 33,
    md5 = "3dc93856e78e39a6f880b627ac535c74",
  )

  @Test
  fun `method == IDENTIFIER`() {
    assertThat(compressionMethod.method).isEqualTo(DeflateCompression.IDENTIFIER)
  }
}
