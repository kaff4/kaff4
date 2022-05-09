dependencies {
  implementation(kotlin("reflect"))

  api(project(":core:core-logging"))
  api(project(":core:core-guice"))

  implementation("org.eclipse.rdf4j:rdf4j-rio-turtle:4.0.0")
  implementation("com.squareup.okio:okio:3.1.0")
}