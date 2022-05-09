package com.github.nava2.aff4.meta

sealed interface Aff4Model {

  abstract class Factory(private val types: List<String>) {

    fun tryCreate(context: ModelRdfContext): Aff4Model? {
      if (!matchesTypes(context)) return null
      return protectedTryCreate(context)
    }

    abstract fun protectedTryCreate(context: ModelRdfContext): Aff4Model?

    private fun matchesTypes(context: ModelRdfContext): Boolean {
      return types.any { Iri.parse(context.namespaces, it) in context.types }
    }
  }
}

//class Aff4InformationModelFactory(
//  private val namespaces: Map<String, String>,
//) {
//  private val statementConverters = mapOf(
//    Urn::class.java to { value: Value -> (value as IRI) },
//    ByteString::class.java to { value: Value -> (value as Literal).stringValue().decodeHex() },
//    Int::class.java to { value: Value -> (value as Literal).intValue() },
//    Long::class.java to { value: Value -> (value as Literal).longValue() },
//  )
//
//  private val factoryMap: Map<TypeKey, (statements: List<Statement>) -> Aff4Model>
//
//  private data class TypeKey(val uris: Set<IRI>)
//
//  init {
//    factoryMap = Aff4Model::class.sealedSubclasses.associate { dataType ->
//      val matchingAnnotation = dataType.annotations.filterIsInstance<Aff4Type>().single()
//      val uris = matchingAnnotation.typeUris.map { uri -> IRI(uri) }.toSet()
//
//      val constructor = dataType.primaryConstructor!!
//      val parameterMap = constructor.parameters.associate { parameter ->
//        val parameterUriString = parameter.annotations.filterIsInstance<Aff4Type>().firstOrNull()?.typeUris?.single()
//          ?: "aff4:${parameter.name}"
//        val parameterPredicateUri = URI(parameterUriString)
//
//        val parameterType = parameter.type.javaType
//        parameterPredicateUri to (statementConverters[parameterType] ?: )
//      }
//
//
//
//      TODO()
//    }
//  }
//
//  private fun
//}