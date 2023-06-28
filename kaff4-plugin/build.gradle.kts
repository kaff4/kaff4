dependencies {
  api(libs.guice)
  api(libs.jakarta.inject.api)
  api(libs.misk.inject)

  api(project("::kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-rdf:kaff4-rdf-api"))

  runtimeOnly(libs.rdf4j.repository.sail)
}
