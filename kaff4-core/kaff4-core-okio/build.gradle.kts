dependencies {
  api(libs.guava)

  implementation(libs.okio)

  testImplementation(project(":kaff4-core:kaff4-core-okio"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()