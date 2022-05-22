dependencies {
  api(project(":core:core-guice"))
  api(project(":core:core-test"))

  implementation(Dependencies.GUICE)
  implementation(Dependencies.OKIO)

  implementation(project(":aff4:aff4-core"))
  implementation(project(":aff4:aff4-core:aff4-core-model"))
  implementation(project(":aff4:aff4-rdf:aff4-rdf-memory"))
}