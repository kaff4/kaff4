dependencies {
  api(Dependencies.GUAVA)

  implementation(Dependencies.OKIO)

  testImplementation(project(":aff4:aff4-core:aff4-core-test"))
}

useJunit5()