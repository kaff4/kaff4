dependencies {
  api(Dependencies.GUAVA)

  implementation(Dependencies.OKIO)

  testImplementation(Dependencies.JAVAX_INJECT)
  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
}

useJunit5()