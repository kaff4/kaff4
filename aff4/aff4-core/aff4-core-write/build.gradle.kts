dependencies {
  api(Dependencies.OKIO)

  api(project(":core:core-guice"))
  api(project(":core:core-logging"))

  implementation(Dependencies.GUAVA)
  implementation(Dependencies.GUICE_ASSISTED_INJECT)

  implementation(project(":aff4:aff4-core"))

  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
}