import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
  `maven-publish`
  signing

  alias(libs.plugins.detekt)
  alias(libs.plugins.dependencyanalysis)
  alias(libs.plugins.kotlin)
  alias(libs.plugins.kotlinx.binary.compatibility.validator)
  alias(libs.plugins.license)
  alias(libs.plugins.nexus.publish)
}

buildscript {
  repositories {
    mavenCentral()
  }
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

apply {
  plugin("io.github.gradle-nexus.publish-plugin")
}

nexusPublishing {
  repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

      username.set(System.getenv("OSSRH_USERNAME"))
      password.set(System.getenv("OSSRH_PASSWORD"))
    }
  }
}

apiValidation {
  ignoredProjects.addAll(
    rootProject.subprojects.map { it.name }.filter { it.endsWith("-test") }
  )
  ignoredProjects.add(
    "kaff4-cli",
  )
}

val isRelease = !System.getenv("RELEASE").isNullOrBlank()

allprojects {
  apply {
    plugin("com.jaredsburrows.license")
  }

  buildscript {
    repositories {
      mavenCentral()
    }
  }

  group = "net.navatwo.kaff4"
  version = "0.0.0-SNAPSHOT"

  if (isRelease) {
    version = version.toString().substringBefore("-SNAPSHOT")
  }

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

val detektFormatting = libs.detekt.formatting

subprojects {
  apply {
    plugin("org.jetbrains.kotlin.jvm")
    plugin("io.gitlab.arturbosch.detekt")
    plugin("maven-publish")
    plugin("signing")
  }

  repositories {
    mavenLocal()
  }

  java {
    withJavadocJar()
    withSourcesJar()
  }

  publishing {
    publications {
      create<MavenPublication>("maven") {
        from(components["java"])

        pom {
          name.set("kaff4")
          description.set("Kotlin implementation the AFF4 standard")
          url.set("https://github.com/Nava2/kaff4")

          licenses {
            license {
              name.set("MIT License")
              url.set("https://opensource.org/licenses/MIT")
            }
          }
          developers {
            developer {
              id.set("Nava2")
              name.set("Kevin Brightwell")
              email.set("kevin.brightwell2+kaff4@gmail.com")
            }
          }
          scm {
            url.set("https://github.com/Nava2/kaff4.git")
          }
        }
      }
    }
  }

  signing {
    fun findProperty(name: String): String? {
      val propertyName = "ORG_GRADLE_PROJECT_$name"
      return project.findProperty(propertyName) as? String
        ?: System.getenv("ORG_GRADLE_PROJECT_$name")
    }

    val signingKeyId: String? = findProperty("signingKeyId")
    val signingKey: String? = findProperty("signingKey")
    val signingPassword: String? = findProperty("signingPassword")

    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)

    sign(publishing.publications["maven"])
  }

  tasks.test {
    useJUnitPlatform()
  }

  detekt {
    parallel = true
    autoCorrect = true

    buildUponDefaultConfig = true // preconfigure defaults
    config.from("$rootDir/config/detekt-config.yml")

    allRules = false // activate all available (even unstable) rules.
  }

  dependencies {
    detektPlugins(detektFormatting)
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