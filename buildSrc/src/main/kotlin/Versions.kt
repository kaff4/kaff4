import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.jvm.toolchain.JvmVendorSpec

object Versions {
  const val JVM_BYTECODE_TARGET = 17
  const val KOTLIN = "1.8.21"
  const val DETEKT = "1.22.0"

  const val JUNIT = "5.9.1"

  const val GUAVA = "31.1-jre"
  const val GUICE = "5.1.0"

  const val MISK = "0.24.0"

  const val LOG4J = "2.17.1"
  const val RDF4J = "4.0.0"

  fun JavaToolchainSpec.configureJavaToolchain() {
    languageVersion.set(JavaLanguageVersion.of(Versions.JVM_BYTECODE_TARGET))
    vendor.set(JvmVendorSpec.ADOPTIUM)
  }
}

