plugins {
  id("kaff4.library")
}

dependencies {
  api(libs.guice)
  api(libs.jakarta.inject.api)

  api(libs.rdf4j.model.api)
  api(libs.rdf4j.sail.api)
}
