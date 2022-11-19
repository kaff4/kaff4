dependencies {
  api(Dependencies.LOG4J_API)

  runtimeOnly(Dependencies.LOG4J_CORE)
  runtimeOnly(Dependencies.LOG4J_SLF4J)
}

useJunit5()