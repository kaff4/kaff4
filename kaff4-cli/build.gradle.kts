import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  application
}

dependencies {
  implementation(libs.guice)
  implementation(libs.jakarta.inject.api)
  implementation(libs.misk.actionscopes)
  implementation(libs.log4j.api)
  implementation(libs.okio)
  implementation(libs.rdf4j.model.api)
  implementation(libs.rdf4j.model)
  implementation(libs.kotlinx.cli)

  implementation(project(":kaff4-core"))
  implementation(project(":kaff4-core:kaff4-core-logging"))
  implementation(project(":kaff4-core:kaff4-core-guice"))
  implementation(project(":kaff4-core:kaff4-core-model"))
  implementation(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  implementation(project(":kaff4-core:kaff4-core-okio"))
  implementation(project(":kaff4-rdf:kaff4-rdf-memory"))
  implementation(project(":kaff4-compression"))
  implementation(project(":kaff4-compression:kaff4-compression-snappy"))
  implementation(project(":kaff4-compression:kaff4-compression-lz4"))
}

tasks.withType<KotlinCompile> {
  kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

application {
  mainClass.set("net.navatwo.kaff4.MainKt")
}

val copyMetaFiles by tasks.registering {
  val meta = layout.buildDirectory.dir("meta")
  outputs.dir(meta)

  doLast {
    copy {
      into(meta)

      from(rootDir) {
        include("LICENSE", "README.md")
      }
    }
  }
}

copy {
  into(layout.buildDirectory.dir("scripts"))
  from(rootDir) {
    include(".java-version")
  }
}

distributions {
  main {
    contents {
      from(copyMetaFiles) {
        into("bin")
      }
    }
  }
}