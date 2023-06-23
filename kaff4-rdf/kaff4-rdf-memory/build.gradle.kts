dependencies {
  api(project(":kaff4-plugin"))

  implementation(libs.guice)
  implementation(libs.javax.inject)
  implementation(libs.misk.inject)
  implementation(libs.rdf4j.sail.api)
  implementation("org.eclipse.rdf4j:rdf4j-sail-memory:4.0.5")

  implementation(project(":kaff4-core:kaff4-core-guice"))
  implementation(project(":kaff4-rdf:kaff4-rdf-api"))
  implementation(project(":kaff4-rdf"))
}