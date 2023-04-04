package com.github.nava2.aff4.rdf.io

import org.apache.commons.lang3.ClassUtils
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType

internal data class PropertyInfo(
  val parameter: KParameter,
  val property: KProperty<*>,
) {
  val collectionType: Class<*>?
  val elementType: Class<*>

  init {
    val javaType = parameter.type.javaType

    val elementType: Type
    if (javaType is ParameterizedType) {
      if (javaType.rawType == List::class.java || javaType.rawType == Set::class.java) {
        collectionType = javaType.rawType as Class<*>
        elementType = when (val argType = javaType.actualTypeArguments.single()) {
          is WildcardType -> argType.upperBounds.single()
          else -> argType
        }
      } else {
        collectionType = null
        elementType = javaType
      }
    } else {
      collectionType = null
      elementType = javaType
    }

    elementType as Class<*>
    this.elementType = if (
      ClassUtils.isPrimitiveOrWrapper(elementType) &&
      ClassUtils.isPrimitiveWrapper(elementType)
    ) {
      ClassUtils.wrapperToPrimitive(elementType)
    } else {
      elementType
    }
  }
}
