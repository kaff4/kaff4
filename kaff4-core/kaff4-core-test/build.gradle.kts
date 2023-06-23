dependencies {
  implementation(kotlin("reflect"))

  api(project(":kaff4-core:kaff4-core-guice"))
  api(project(":kaff4-core:kaff4-core-model"))
  api(project(":kaff4-core:kaff4-core-okio"))

  api("org.assertj:assertj-core:3.22.0")
  api("com.google.inject:guice:7.0.0")
  api("javax.inject:javax.inject:1")
  api("org.junit.jupiter:junit-jupiter-api:5.9.3")
  api("com.squareup.okio:okio:3.3.0")

  implementation("com.squareup.misk:misk-inject:0.24.0")
  implementation("com.squareup.misk:misk-action-scopes:0.25.0-20230405.1913-51e097f")

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
