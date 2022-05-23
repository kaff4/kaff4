dependencies {
  implementation(kotlin("reflect"))

  api(Dependencies.OKIO)

  api(project(":core:core-logging"))
  api(project(":core:core-guice"))
  api(project(":aff4:aff4-core:aff4-core-model"))

  implementation(project(":aff4:aff4-rdf"))

  implementation(project(":aff4:aff4-core:aff4-core-okio"))
  implementation(project(":aff4:aff4-core:aff4-core-interval-tree"))

  implementation(Dependencies.APACHE_COMMONS_LANG)
  implementation(Dependencies.GUICE_ASSISTED_INJECT)
  implementation(Dependencies.CAFFIENE)

  testImplementation(project(":aff4:aff4-compression-snappy"))
  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
  testImplementation(project(":aff4:aff4-rdf:aff4-rdf-memory"))
}