dependencies {
  api(Dependencies.GUAVA)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.OKIO)
  api(Dependencies.RDF4J_MODEL_API)
  api(Dependencies.RDF4J_QUERY)

  api(project(":kaff4-core:kaff4-core-guice"))

  implementation(Dependencies.GUICE)
  implementation(Dependencies.MISK_INJECT)

  implementation(kotlin("reflect"))
}

useJunit5()