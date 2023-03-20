import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
  implementation(Dependencies.GUICE)
  implementation(Dependencies.JAVAX_INJECT)
  implementation(Dependencies.LOG4J_API)
  implementation(Dependencies.OKIO)
  implementation(Dependencies.RDF4J_MODEL_API)

  implementation(project(":aff4:aff4-core"))
  implementation(project(":aff4:aff4-core:aff4-core-logging"))
  implementation(project(":aff4:aff4-core:aff4-core-guice"))
  implementation(project(":aff4:aff4-core:aff4-core-model"))
  implementation(project(":aff4:aff4-core:aff4-core-model:aff4-core-model-api"))
  implementation(project(":aff4:aff4-core:aff4-core-okio"))
  implementation(project(":aff4:aff4-rdf:aff4-rdf-memory"))
  implementation(project(":aff4:aff4-compression:aff4-compression-snappy"))

  implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
}

tasks.withType<KotlinCompile> {
  kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}
