import org.gradle.api.Project

fun Project.useJunit5() {
  dependencies.add("testImplementation", Dependencies.ASSERTJ_CORE)

  dependencies.add("testImplementation", Dependencies.JUNIT_JUIPTER_API)
  dependencies.add("testRuntimeOnly", Dependencies.JUNIT_JUIPTER_ENGINE)
}