package com.github.nava2.test

import com.github.nava2.aff4.io.Sha256FileSystemFactory
import com.github.nava2.guice.GuiceFactory
import com.github.nava2.guice.KAbstractModule
import com.github.nava2.guice.getInstance
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaType

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class GuiceModule(
  vararg val modules: KClass<out Module>,
)

class GuiceExtension : BeforeEachCallback, AfterEachCallback, BeforeAllCallback {

  private val modulePropertyMaps = ConcurrentHashMap<Class<*>, (Any) -> Collection<Module>>()

  private val testInjectors = ConcurrentHashMap<Method, Injector>()

  override fun beforeAll(context: ExtensionContext) {
    val testClass = context.requiredTestClass

    modulePropertyMaps.getOrPut(testClass) {
      val properties = testClass.kotlin.getModuleProperties()

      return@getOrPut { instance ->
        val fromClass by lazy {
          testClass.kotlin.findAnnotation<GuiceModule>()?.modules?.map { getModuleInstance(it) }
            ?: listOf()
        }

        fromClass + properties.flatMap { property ->
          val fromAnnotation by lazy(LazyThreadSafetyMode.NONE) {
            property.findAnnotation<GuiceModule>()!!.modules.map { getModuleInstance(it) }
          }

          val returnType = property.returnType
          val fromProperty = when {
            returnType.isModule -> {
              val fromProperty = property.getter.call(instance) as Module
              listOf(fromProperty)
            }

            returnType.isModuleCollection -> {
              @Suppress("UNCHECKED_CAST")
              property.getter.call(instance) as Collection<Module>
            }

            else -> error("Impossible")
          }

          fromAnnotation + fromProperty
        }
      }
    }
  }

  private fun getModuleInstance(mKlass: KClass<out Module>): Module {
    return if (mKlass.objectInstance != null) {
      mKlass.objectInstance!!
    } else {
      val primaryConstructor = mKlass.primaryConstructor
      checkNotNull(primaryConstructor)
      check(primaryConstructor.parameters.isEmpty()) {
        "Must have default constructor: ${mKlass.qualifiedName}"
      }

      primaryConstructor.call()
    }
  }

  override fun beforeEach(context: ExtensionContext) {
    val instance = context.requiredTestInstance
    val providedModulesSupplier = modulePropertyMaps.getValue(context.requiredTestClass)
    val providedModules = providedModulesSupplier(instance)

    val modules = BASE_MODULES + providedModules

    val injector = testInjectors.computeIfAbsent(context.requiredTestMethod) {
      Guice.createInjector(Stage.DEVELOPMENT, modules)
    }

    injector.injectMembers(instance)
  }

  override fun afterEach(context: ExtensionContext) {
    context.testMethod.ifPresent { method ->
      val injector = testInjectors[method] ?: return@ifPresent
      val cleanupActions = injector.getInstance<CleanupActions>()
      cleanupActions.close()

      testInjectors.remove(method)
    }
  }

  companion object {
    private val BASE_MODULES = listOf<Module>(
      TestModule,
    )
  }

  @Singleton
  class CleanupActions @Inject internal constructor() : AutoCloseable {
    private val cleanups = mutableListOf<() -> Unit>()

    fun register(action: () -> Unit) {
      cleanups += action
    }

    override fun close() {
      for (action in cleanups) {
        action()
      }
    }
  }

  private object TestModule : KAbstractModule() {
    override fun configure() {
      bind<Sha256FileSystemFactory>().toProvider(Provider { Sha256FileSystemFactory() })
      bind<CleanupActions>()
      bind<GuiceFactory>().toInstance(
        object : GuiceFactory {
          override fun create(modules: Collection<Module>): Injector {
            return Guice.createInjector(Stage.DEVELOPMENT, modules)
          }
        }
      )
    }
  }
}

private fun KClass<*>.getModuleProperties(): List<KProperty<*>> {
  return declaredMemberProperties.asSequence()
    .filter { it.hasAnnotation<GuiceModule>() }
    .onEach { property ->
      val returnType = property.returnType

      if (returnType.isModule || returnType.isModuleCollection) {
        property.javaGetter?.isAccessible = true
      } else {
        error(
          "@${GuiceModule::class.simpleName} only applies to ${Module::class.qualifiedName} or " +
            "Collection<${Module::class.simpleName}>",
        )
      }
    }
    .toList()
}

private val KType.isModule: Boolean
  get() {
    return (javaType as? Class<*>)?.let { Module::class.java.isAssignableFrom(it) } == true
  }

private val KType.isModuleCollection: Boolean
  get() {
    val parameterizedType = javaType as? ParameterizedType ?: return false

    val innerType = parameterizedType.actualTypeArguments.singleOrNull() as? Class<*>
      ?: return false
    val collectionClass = parameterizedType.rawType as? Class<*> ?: return false
    return Collection::class.java.isAssignableFrom(collectionClass) &&
      Module::class.java.isAssignableFrom(innerType)
  }
