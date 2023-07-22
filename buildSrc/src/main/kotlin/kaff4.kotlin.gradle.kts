import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.provideDelegate

plugins {
  id("org.jetbrains.kotlin.jvm")
}

group = "net.navatwo.kaff4"
version = "0.0.0-SNAPSHOT"

val isRelease = !System.getenv("RELEASE").isNullOrBlank()

if (isRelease) {
  version = version.toString().substringBefore("-SNAPSHOT")
}

repositories {
  mavenCentral()
}

fun JavaToolchainSpec.configureJavaToolchain() {
  languageVersion.set(JavaLanguageVersion.of(18))
  vendor.set(JvmVendorSpec.ADOPTIUM)
}

java {
  withJavadocJar()
  withSourcesJar()

  toolchain {
    configureJavaToolchain()
  }
}

kotlin {
  jvmToolchain {
    configureJavaToolchain()
  }
}

tasks.test {
  useJUnitPlatform()

  jvmArgumentProviders.add { listOf("-Djunit.jupiter.extensions.autodetection.enabled=true") }
}

tasks.named("check") {
  dependsOn(tasks.named("projectHealth"))
}
