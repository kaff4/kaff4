dependencies {
  api(Dependencies.GUAVA)

  implementation(Dependencies.OKIO)

  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()