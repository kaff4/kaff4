object Versions {
  const val JVM_BYTECODE_TARGET = "11"
  const val JVM_TARGET = "17"
  const val KOTLIN = "1.6.21"
  const val DETEKT = "1.20.0"

  const val JUNIT = "5.9.1"

  const val GUAVA = "31.1-jre"
  const val GUICE = "5.1.0"

  const val LOG4J = "2.17.1"
}

object Dependencies {
  const val APACHE_COMMONS_LANG = "org.apache.commons:commons-lang3:3.12.0"

  const val ASSERTJ_CORE = "org.assertj:assertj-core:3.22.0"
  const val JUNIT_JUIPTER_API = "org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}"
  const val JUNIT_JUIPTER_ENGINE = "org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}"

  const val CAFFIENE = "com.github.ben-manes.caffeine:caffeine:3.1.0"

  const val GUAVA = "com.google.guava:guava:${Versions.GUAVA}"

  const val GUICE = "com.google.inject:guice:${Versions.GUICE}"
  const val GUICE_ASSISTED_INJECT = "com.google.inject.extensions:guice-assistedinject:${Versions.GUICE}"

  const val JAVAX_INJECT = "javax.inject:javax.inject:1"

  const val JODA_TIME = "joda-time:joda-time:2.10.14"
  const val JUNIT4 = "junit:junit:4.13.2"

  const val LOG4J_API = "org.apache.logging.log4j:log4j-api:${Versions.LOG4J}"
  const val LOG4J_CORE = "org.apache.logging.log4j:log4j-core:${Versions.LOG4J}"
  const val LOG4J_SLF4J = "org.apache.logging.log4j:log4j-slf4j-impl:${Versions.LOG4J}"

  const val OKIO = "com.squareup.okio:okio:3.1.0"
}