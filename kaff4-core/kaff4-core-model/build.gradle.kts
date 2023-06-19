dependencies {
  compileOnly(kotlin("reflect"))

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-core:kaff4-core-okio"))
  api(project(":kaff4-plugin"))
  api(project(":kaff4-rdf"))

  api("com.google.inject:guice:5.1.0")
  api("javax.inject:javax.inject:1")
  api("com.squareup.okio:okio:3.1.0")
  api("org.eclipse.rdf4j:rdf4j-model-api:4.3.2")

  implementation("com.squareup.misk:misk-inject:0.24.0")

  testImplementation("org.eclipse.rdf4j:rdf4j-model:4.3.2")

  testImplementation(project(":kaff4-core:kaff4-core-model"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()
