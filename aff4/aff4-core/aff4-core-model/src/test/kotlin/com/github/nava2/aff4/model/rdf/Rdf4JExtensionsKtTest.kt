package com.github.nava2.aff4.model.rdf

import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.junit.Test

class Rdf4JExtensionsKtTest {
  private val valueFactory = SimpleValueFactory.getInstance()

  @Test
  fun `not container rooted paths`() {
    val containerIri = valueFactory.createArn("aff4://685e15cc-d0fb-4dbc-ba47-48117fc77044")
    val path = valueFactory.createArn("aff4://fcbfdce7-4488-4677-abf6-08bc931e195b/idx")
    assertThat(path.toAff4Path(containerIri))
      .isEqualTo("aff4%3A%2F%2Ffcbfdce7-4488-4677-abf6-08bc931e195b/idx".toPath())
  }

  @Test
  fun `container rooted paths`() {
    val containerIri = valueFactory.createArn("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83")
    val path = valueFactory.createArn("aff4://5aea2dd0-32b4-4c61-a9db-677654be6f83//test_images/AFF4-L/dream.txt")
    assertThat(path.toAff4Path(containerIri))
      .isEqualTo("//test_images/AFF4-L/dream.txt".toPath())
  }
}
