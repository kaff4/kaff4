dependencies {
  api(libs.log4j.api)

  runtimeOnly(libs.log4j.core)
  runtimeOnly(libs.log4j.slf4j.impl)

  testImplementation(libs.assertj)
  testImplementation(libs.junit.juipter.api)

  testRuntimeOnly(libs.junit.juipter.engine)
}