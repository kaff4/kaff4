dependencies {
  implementation(kotlin("reflect"))

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-rdf:kaff4-rdf-api"))

  api(libs.guice)
  api(libs.jakarta.inject.api)
  api(libs.rdf4j.model.api)
  api(libs.rdf4j.repository.api)
  api(libs.rdf4j.rio.api)
  api(libs.rdf4j.query)

  implementation(libs.commons.lang3)
  implementation(libs.caffeine)
  implementation(libs.checker.qual)
  implementation(libs.guava)
  implementation(libs.guice.assistedInject)
  implementation(libs.misk.inject)
  implementation(libs.misk.actionscopes)
  implementation(libs.rdf4j.repository.sail)
  implementation(libs.rdf4j.rio.turtle)
  implementation(libs.rdf4j.sail.api)

  implementation(project(":kaff4-core:kaff4-core-kotlin"))

  testImplementation(project(":kaff4-core"))
  testImplementation(project(":kaff4-core:kaff4-core-model"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
  testImplementation(project(":kaff4-rdf"))
  testImplementation(project(":kaff4-rdf:kaff4-rdf-memory"))
}

useJunit5()