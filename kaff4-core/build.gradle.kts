dependencies {
  implementation(kotlin("reflect"))

  api(libs.guice)
  api(libs.javax.inject)
  api(libs.okio)
  api(libs.rdf4j.model.api)
  api(libs.kintervaltree)

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  api(project(":kaff4-core:kaff4-core-model"))
  api(project(":kaff4-core:kaff4-core-okio"))

  implementation(libs.caffeine)
  implementation(libs.checker.qual)
  implementation(libs.guava)
  implementation(libs.guice.assistedInject)
  implementation(libs.misk.inject)
  implementation(libs.misk.actionscopes)
  implementation(libs.rdf4j.query)
  implementation(libs.rdf4j.repository.api)
  implementation(libs.rdf4j.rio.api)
  implementation(libs.fastcdc4j)

  implementation(project(":kaff4-core:kaff4-core-kotlin"))
  implementation(project(":kaff4-rdf"))

  testImplementation(libs.junit.juipter.params)

  testImplementation(project(":kaff4-rdf:kaff4-rdf-memory"))
  testImplementation(project(":kaff4-compression"))
  testImplementation(project(":kaff4-compression:kaff4-compression-lz4"))
  testImplementation(project(":kaff4-compression:kaff4-compression-snappy"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()
