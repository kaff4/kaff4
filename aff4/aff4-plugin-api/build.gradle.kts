dependencies {
  api(Dependencies.GUICE)
  api(Dependencies.RDF4J_MODEL_API)

  api(project(":aff4:aff4-compression:aff4-compression-api"))
  api(project(":aff4:aff4-core:aff4-core-guice"))

  api(Dependencies.RDF4J_REPOSITORY_SAIL)
}
