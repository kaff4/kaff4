dependencies {
  implementation(kotlin("reflect"))

  api(project(":core:core-logging"))
  api(project(":core:core-guice"))

  implementation(Dependencies.GUICE_ASSISTED_INJECT)
  implementation("org.apache.commons:commons-lang3:3.12.0")

  implementation("org.eclipse.rdf4j:rdf4j-rio-turtle:4.0.0")
  implementation("org.eclipse.rdf4j:rdf4j-repository-api:4.0.0")
  implementation("org.eclipse.rdf4j:rdf4j-repository-sail:4.0.0")
  implementation("org.eclipse.rdf4j:rdf4j-sail-memory:4.0.0")
  implementation("org.eclipse.rdf4j:rdf4j-query:4.0.0")
  implementation("com.squareup.okio:okio:3.1.0")

  // TODO new module
  implementation("org.xerial.snappy:snappy-java:1.1.8.4")

}