plugins {
  id("kaff4.library")
}

dependencies {
  api(project(":kaff4-api:kaff4-api-features"))

  compileOnly(project(":kaff4-api"))

  implementation(libs.guice)
  implementation(libs.jakarta.inject.api)
  implementation(libs.misk.inject)
  implementation(libs.rdf4j.sail.api)
  implementation(libs.rdf4j.sail.memory)

  implementation(project(":kaff4-rdf:kaff4-rdf-api"))
  implementation(project(":kaff4-rdf"))
}