import org.gradle.api.Project

fun Project.useJunit5() {
  dependencies.add("testImplementation", "org.assertj:assertj-core:3.22.0")

  dependencies.add("testImplementation", "org.junit.jupiter:junit-jupiter-api:5.9.1")
  dependencies.add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:5.9.1")
}
