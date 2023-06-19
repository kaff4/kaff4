import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  application
}

dependencies {
  implementation("com.google.inject:guice:5.1.0")
  implementation("javax.inject:javax.inject:1")
  implementation("com.squareup.misk:misk-action-scopes:0.25.0-20230405.1913-51e097f")
  implementation("org.apache.logging.log4j:log4j-api:2.17.1")
  implementation("com.squareup.okio:okio:3.1.0")
  implementation("org.eclipse.rdf4j:rdf4j-model-api:4.0.0")
  implementation("org.eclipse.rdf4j:rdf4j-model:4.0.0")
  implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")

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