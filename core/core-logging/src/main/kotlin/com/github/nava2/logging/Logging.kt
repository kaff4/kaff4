package com.github.nava2.logging

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.invoke.MethodHandles
import kotlin.reflect.KProperty

object Logging {
  operator fun getValue(instance: Any, property: KProperty<*>): Logger {
    return LogManager.getLogger(instance::class.java)
  }

  @Suppress(
    "NOTHING_TO_INLINE", // Requires inline for MethodHandles.lookup()
    "SwallowedException", // We intentionally swallow the ClassNotFoundException as it shouldn't be called
  )
  inline operator fun getValue(nothing: Nothing?, property: KProperty<*>): Logger {
    val lookup = MethodHandles.lookup()
    val clazz = lookup.lookupClass()

    val realClazz = if (clazz.simpleName.endsWith("Kt")) {
      try {
        clazz.classLoader.loadClass(clazz.typeName.substringBeforeLast("Kt"))
      } catch (cnfe: ClassNotFoundException) {
        clazz
      }
    } else {
      clazz
    }

    return LogManager.getLogger(realClazz)
  }
}
