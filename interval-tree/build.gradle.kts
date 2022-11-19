plugins {
  `maven-publish`
}

dependencies {
  implementation(Dependencies.GUAVA)
}

useJunit5()

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["kotlin"])
      pom {
        licenses {
          clear()
          license {
            name.set("MIT License")
            url.set("https://opensource.org/licenses/MIT")
          }
        }
        developers {
          developer {
            name.set("Mason M Lai")
          }
        }
        scm {
          url.set("https://github.com/charcuterie/interval-tree/")
        }
      }
    }
  }
}