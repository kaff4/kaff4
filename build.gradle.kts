import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version Versions.KOTLIN
  id("io.gitlab.arturbosch.detekt") version Versions.DETEKT
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
    plugin("org.jetbrains.kotlin.jvm")
    plugin("io.gitlab.arturbosch.detekt")
  }

  buildscript {
    repositories {
      mavenCentral()
    }
  }

  repositories {
    mavenCentral()
  }

  group = "com.github.nava2.kaff4"

  dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation(Dependencies.ASSERTJ_CORE)
    
    if (project.name != "aff4-core-test") {
      testImplementation(project(":aff4:aff4-core:aff4-core-test"))
    }
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

  tasks.withType<Detekt>().configureEach {
    jvmTarget = Versions.JVM_BYTECODE_TARGET
  }
  tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = Versions.JVM_BYTECODE_TARGET
  }

  tasks.withType<Detekt>().configureEach {
    reports {
      html.required.set(true) // observe findings in your browser with structure and code snippets
      xml.required.set(true) // checkstyle like format mainly for integrations like Jenkins
      txt.required.set(true) // similar to the console output, contains issue signature to manually edit baseline files
      sarif.required.set(true) // standardized SARIF format (https://sarifweb.azurewebsites.net/) to support integrations with Github Code Scanning
    }
  }
}