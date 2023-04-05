dependencies {
  implementation(kotlin("stdlib-jdk8"))

  api(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))

  api(Dependencies.JUNIT_JUIPTER_API)

  implementation(project(":kaff4-compression"))
  implementation(project(":kaff4-core:kaff4-core-test"))

  implementation(Dependencies.ASSERTJ_CORE)
  implementation(Dependencies.OKIO)
}