dependencies {
  api(libs.guava)
  api(libs.jakarta.inject.api)
  api(libs.okio)
  api(libs.rdf4j.model.api)
  api(libs.rdf4j.query)

  api(project(":kaff4-core:kaff4-core-guice"))

  implementation(libs.guice)
  implementation(libs.misk.inject)

  implementation(kotlin("reflect"))

  testImplementation(libs.assertj)
  testImplementation(libs.junit.juipter.api)

  testRuntimeOnly(libs.junit.juipter.engine)
}