dependencies {
  api("com.google.guava:guava:31.1-jre")
  api("javax.inject:javax.inject:1")
  api("com.squareup.okio:okio:3.3.0")
  api("org.eclipse.rdf4j:rdf4j-model-api:4.0.5")
  api("org.eclipse.rdf4j:rdf4j-query:4.3.2")

  api(project(":kaff4-core:kaff4-core-guice"))

  implementation("com.google.inject:guice:5.1.0")
  implementation("com.squareup.misk:misk-inject:0.24.0")

  implementation(kotlin("reflect"))
}

useJunit5()