dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(Dependencies.JAVAX_INJECT)

  api(project(":kaff4-plugin"))
  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))

  implementation(Dependencies.GUICE)
  implementation(Dependencies.MISK_INJECT)
  implementation("org.apache.commons:commons-compress:1.23.0")

  implementation(project(":kaff4-core:kaff4-core-guice"))
  implementation(project(":kaff4-compression"))

  testImplementation(Dependencies.OKIO)
  
  testImplementation(project(":kaff4-core:kaff4-core-test"))
}

useJunit5()