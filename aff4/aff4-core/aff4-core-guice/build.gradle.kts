dependencies {
  runtimeOnly(kotlin("reflect"))

  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)

  implementation(Dependencies.GUAVA)
  implementation(Dependencies.GUICE_ASSISTED_INJECT)

  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
}

useJunit5()