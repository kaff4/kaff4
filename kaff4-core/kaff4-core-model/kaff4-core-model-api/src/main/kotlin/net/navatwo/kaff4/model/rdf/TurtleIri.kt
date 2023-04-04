package net.navatwo.kaff4.model.rdf

@JvmInline
value class TurtleIri private constructor(val iri: String) {
  init {
    val index = iri.indexOf(':', startIndex = 1)
    check(index in (0 until iri.lastIndex)) {
      "Must use valid IRI syntax for Turtle: \$NAMESPACE:\$LOCAL_NAME (e.g. aff4:Image)"
    }
  }

  val namespace: String
    get() = iri.substringBefore(':')
  operator fun component1(): String = namespace

  val localName: String
    get() = iri.substringAfter(':')
  operator fun component2(): String = localName

  companion object {
    val RDF_TYPE = "rdf:type".toTurtleIri()

    fun String.toTurtleIri() = TurtleIri(this)
  }
}
