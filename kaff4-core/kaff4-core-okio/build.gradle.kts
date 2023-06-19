dependencies {
  api("com.google.guava:guava:31.1-jre")

  implementation("com.squareup.okio:okio:3.1.0")

  testImplementation(project(":kaff4-core:kaff4-core-okio"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()