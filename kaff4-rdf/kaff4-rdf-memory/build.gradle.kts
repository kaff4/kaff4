dependencies {
  api(project(":kaff4-plugin"))

  implementation(libs.guice)
  implementation(libs.jakarta.inject.api)
  implementation(libs.misk.inject)
  implementation(libs.rdf4j.sail.api)
  implementation(libs.rdf4j.sail.memory)

  implementation(project(":kaff4-core:kaff4-core-guice"))
  implementation(project(":kaff4-rdf:kaff4-rdf-api"))
  implementation(project(":kaff4-rdf"))
}