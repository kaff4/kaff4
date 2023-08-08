plugins {
  id("kaff4.library")
}

dependencies {
  api(libs.guava)

  implementation(libs.okio)

  implementation(project(":kaff4-core:kaff4-core-sources"))

  compileOnly(project(":kaff4-api"))

  testImplementation(libs.assertj)
  testImplementation(libs.junit.juipter.api)

  testImplementation(project(":kaff4-core:kaff4-core-okio"))
  testImplementation(project(":kaff4-core:kaff4-core-test"))

  testRuntimeOnly(libs.junit.juipter.engine)
}