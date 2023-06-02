dependencies {
  compileOnly(kotlin("reflect"))

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-core:kaff4-core-okio"))
  api(project(":kaff4-plugin"))
  api(project(":kaff4-rdf"))

  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.OKIO)
  api(Dependencies.RDF4J_MODEL_API)

  implementation(Dependencies.MISK_INJECT)

  testImplementation(Dependencies.RDF4J_MODEL)

  testImplementation(project(":kaff4-core:kaff4-core-model"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()
