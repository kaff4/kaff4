import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
  `maven-publish`
  signing

  alias(libs.plugins.dependencyanalysis)
  alias(libs.plugins.kotlinx.binary.compatibility.validator)
  alias(libs.plugins.nexus.publish)
}

buildscript {
  repositories {
    mavenCentral()
  }
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
  nonPublicMarkers.add("net.navatwo.kaff4.api.InternalApi")

  ignoredProjects.addAll(
    rootProject.subprojects.map { it.name }.filter { it.endsWith("-test") }
  )
  ignoredProjects.add(
    "kaff4-cli",
  )
}
