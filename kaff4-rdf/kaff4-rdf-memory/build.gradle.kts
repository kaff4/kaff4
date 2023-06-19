dependencies {
  api(project(":kaff4-plugin"))

  implementation("com.google.inject:guice:5.1.0")
  implementation("javax.inject:javax.inject:1")
  implementation("com.squareup.misk:misk-inject:0.24.0")
  implementation("org.eclipse.rdf4j:rdf4j-sail-api:4.0.5")
  implementation("org.eclipse.rdf4j:rdf4j-sail-memory:4.0.5")

  implementation(project(":kaff4-core:kaff4-core-guice"))
  implementation(project(":kaff4-rdf:kaff4-rdf-api"))
  implementation(project(":kaff4-rdf"))
}