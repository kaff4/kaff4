import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.java
import org.gradle.kotlin.dsl.publishing
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.signing

plugins {
  id("kaff4.base")

  `maven-publish`
  signing
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])

      pom {
        name.set("kaff4")
        description.set("Kotlin implementation the AFF4 standard")
        url.set("https://github.com/Nava2/kaff4")

        licenses {
          license {
            name.set("MIT License")
            url.set("https://opensource.org/licenses/MIT")
          }
        }
        developers {
          developer {
            id.set("Nava2")
            name.set("Kevin Brightwell")
            email.set("kevin.brightwell2+kaff4@gmail.com")
          }
        }
        scm {
          url.set("https://github.com/Nava2/kaff4.git")
        }
      }
    }
  }
}

signing {
  fun findProperty(name: String): String? {
    val propertyName = "ORG_GRADLE_PROJECT_$name"
    return project.findProperty(propertyName) as? String
      ?: System.getenv("ORG_GRADLE_PROJECT_$name")
  }

  val signingKeyId: String? = findProperty("signingKeyId")
  val signingKey: String? = findProperty("signingKey")
  val signingPassword: String? = findProperty("signingPassword")

  useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)

  sign(publishing.publications["maven"])
}