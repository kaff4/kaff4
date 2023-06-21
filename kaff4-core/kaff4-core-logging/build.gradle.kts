dependencies {
  api("org.apache.logging.log4j:log4j-api:2.17.1")

  runtimeOnly("org.apache.logging.log4j:log4j-core:2.20.0")
  runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")
}

useJunit5()