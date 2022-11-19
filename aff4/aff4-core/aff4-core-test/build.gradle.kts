dependencies {
  implementation(kotlin("reflect"))

  api(project(":aff4:aff4-core"))
  api(project(":aff4:aff4-core:aff4-core-guice"))
  api(project(":aff4:aff4-core:aff4-core-okio"))

  api(Dependencies.ASSERTJ_CORE)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.JUNIT_JUIPTER_API)
  api(Dependencies.GUICE)
  api(Dependencies.OKIO)

  implementation(project(":aff4:aff4-core:aff4-core-model"))
  implementation(project(":aff4:aff4-rdf:aff4-rdf-memory"))
}