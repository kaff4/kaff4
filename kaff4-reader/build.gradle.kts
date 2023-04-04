import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
  implementation(Dependencies.GUICE)
  implementation(Dependencies.JAVAX_INJECT)
  implementation(Dependencies.MISK_ACTION_SCOPES)
  implementation(Dependencies.LOG4J_API)
  implementation(Dependencies.OKIO)
  implementation(Dependencies.RDF4J_MODEL_API)
  implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")

  implementation(project(":kaff4-core"))
  implementation(project(":kaff4-core:kaff4-core-logging"))
  implementation(project(":kaff4-core:kaff4-core-guice"))
  implementation(project(":kaff4-core:kaff4-core-model"))
  implementation(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  implementation(project(":kaff4-core:kaff4-core-okio"))
  implementation(project(":kaff4-rdf:kaff4-rdf-memory"))
  implementation(project(":kaff4-compression:kaff4-compression-snappy"))
}

tasks.withType<KotlinCompile> {
  kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}
