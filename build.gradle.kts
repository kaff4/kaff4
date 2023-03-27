import Versions.configureJavaToolchain
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
  kotlin("jvm") version Versions.KOTLIN
  id("io.gitlab.arturbosch.detekt") version Versions.DETEKT
  id("com.jaredsburrows.license") version "0.9.0"
  id("com.autonomousapps.dependency-analysis") version "1.19.0"
  `maven-publish`
}

buildscript {
  repositories {
    mavenCentral()
  }
}

java {
  toolchain {
    configureJavaToolchain()
  }
}

kotlin {
  jvmToolchain {
    configureJavaToolchain()
  }
}

allprojects {
  apply {
    plugin("com.jaredsburrows.license")
  }

  buildscript {
    repositories {
      mavenCentral()
    }
  }

  group = "com.github.nava2.kaff4"
  version = "0.0.0"

  repositories {
    mavenCentral()
    maven {
      url = uri("https://s01.oss.sonatype.org/content/repositories/releases/")
    }
  }

  licenseReport {
    generateTextReport = true
    generateHtmlReport = true
    generateCsvReport = false
    generateJsonReport = false
  }
}

subprojects {
  apply {
    plugin("org.jetbrains.kotlin.jvm")
    plugin("io.gitlab.arturbosch.detekt")
    plugin("maven-publish")
  }

  repositories {
    mavenLocal()
  }

  publishing {
    publications {
      create<MavenPublication>("maven") {
        from(components["kotlin"])

        pom {
          licenses {
            license {
              name.set("MIT License")
              url.set("https://opensource.org/licenses/MIT")
            }
          }
          scm {
            connection.set("scm:git:https://github.com/Nava2/kaff4.git")
            developerConnection.set("scm:git:ssh://github.com:Nava2/kaff4.git")
            url.set("https://github.com/Nava2/kaff4")
          }
        }
      }
    }
  }

  tasks.test {
    useJUnitPlatform()
  }

  detekt {
    parallel = true
    autoCorrect = true

    buildUponDefaultConfig = true // preconfigure defaults
    config = files("$rootDir/config/detekt-config.yml")

    allRules = false // activate all available (even unstable) rules.
  }

  dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${Versions.DETEKT}")
  }

  tasks.named("check") {
    dependsOn(tasks.named("projectHealth"))
  }

  tasks.withType<Detekt>().configureEach {
    jvmTarget = "18"
  }
  tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "18"
  }

  tasks.withType<Detekt>().configureEach {
    reports {
      html.required.set(true)
    }
  }
}
