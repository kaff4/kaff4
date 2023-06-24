dependencies {
  implementation(kotlin("reflect"))

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model"))
  api(project(":kaff4-core:kaff4-core-okio"))

  api(libs.assertj)
  api(libs.guice)
  api(libs.javax.inject)
  api(libs.junit.juipter.api)
  api(libs.okio)

  implementation(libs.misk.inject)
  implementation(libs.misk.actionscopes)

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
