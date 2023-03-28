dependencies {
  compileOnly(kotlin("reflect"))

  api(project(":aff4:aff4-core:aff4-core-guice"))
  api(project(":aff4:aff4-core:aff4-core-model:aff4-core-model-api"))
  api(project(":aff4:aff4-core:aff4-core-okio"))
  api(project(":aff4:aff4-plugin"))
  api(project(":aff4:aff4-rdf"))

  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.OKIO)
  api(Dependencies.RDF4J_MODEL_API)

  implementation(Dependencies.MISK_INJECT)

  testImplementation(Dependencies.RDF4J_MODEL)

  testImplementation(project(":guice-action-scoped"))
  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
}

useJunit5()
