import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.jvm.toolchain.JvmVendorSpec

object Versions {

  fun JavaToolchainSpec.configureJavaToolchain() {
    languageVersion.set(JavaLanguageVersion.of(18))
    vendor.set(JvmVendorSpec.ADOPTIUM)
  }
}

