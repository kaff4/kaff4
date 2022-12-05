import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version Versions.KOTLIN
  id("io.gitlab.arturbosch.detekt") version Versions.DETEKT
  id("com.jaredsburrows.license") version "0.9.0"
  id("com.autonomousapps.dependency-analysis") version "1.14.1"
  `maven-publish`
}

buildscript {
  repositories {
    mavenCentral()
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(Versions.JVM_BYTECODE_TARGET))
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

  if (project.name != "interval-tree") {
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
  }

  tasks.test {
    useJUnitPlatform()
  }

  val compileKotlin: KotlinCompile by tasks
  compileKotlin.kotlinOptions {
    jvmTarget = Versions.JVM_BYTECODE_TARGET
  }

  val compileTestKotlin: KotlinCompile by tasks
  compileTestKotlin.kotlinOptions {
    jvmTarget = Versions.JVM_BYTECODE_TARGET
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
    jvmTarget = Versions.JVM_BYTECODE_TARGET
  }
  tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = Versions.JVM_BYTECODE_TARGET
  }

  tasks.withType<Detekt>().configureEach {
    reports {
      html.required.set(true)
    }
  }
}
