dependencies {
  api(project(":aff4:aff4-core:aff4-core-guice"))

  api(Dependencies.ASSERTJ_CORE)
  api(Dependencies.GUICE)
  api(Dependencies.OKIO)

  implementation(project(":aff4:aff4-core"))
  implementation(project(":aff4:aff4-core:aff4-core-model"))
  implementation(project(":aff4:aff4-rdf:aff4-rdf-memory"))
}