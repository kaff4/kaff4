dependencies {
  implementation(kotlin("reflect"))

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model"))
  api(project(":kaff4-core:kaff4-core-okio"))

  api(Dependencies.ASSERTJ_CORE)
  api(Dependencies.GUICE)
  api(Dependencies.JAVAX_INJECT)
  api(Dependencies.JUNIT_JUIPTER_API)
  api(Dependencies.OKIO)

  implementation(Dependencies.MISK_INJECT)
  implementation(Dependencies.MISK_ACTION_SCOPES)

  implementation(project(":kaff4-core"))
  implementation(project(":kaff4-core:kaff4-core-model:kaff4-core-model-api"))
  implementation(project(":kaff4-rdf:kaff4-rdf-memory"))
}

val projectPath = project.path

rootProject.allprojects {
  val testTask by tasks.test

  dependencies {
    val thisProject = project(projectPath)
    val testImplementationDependencies = configurations.getByName("testImplementation").dependencies
    if (thisProject in testImplementationDependencies) {
      testTask.jvmArgumentProviders.add { listOf("-Djunit.jupiter.extensions.autodetection.enabled=true") }
    }
  }
}