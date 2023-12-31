plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
  implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.4")
  implementation("com.jaredsburrows:gradle-license-plugin:0.9.3")
}
