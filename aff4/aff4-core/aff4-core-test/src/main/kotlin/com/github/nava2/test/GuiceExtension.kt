package com.github.nava2.test

import com.github.nava2.aff4.io.Sha256FileSystemFactory
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
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import java.lang.reflect.ParameterizedType
import javax.inject.Provider
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaType

@Target(AnnotationTarget.PROPERTY)
annotation class GuiceModule

class GuiceExtension : BeforeEachCallback, AfterEachCallback, BeforeAllCallback {

  override fun beforeAll(context: ExtensionContext) {
    val testClass = context.requiredTestClass

    val store = context.getStore(getClassLevelNamespace(context))

    store.getOrComputeIfAbsent(testClass) {
      val properties = testClass.kotlin.getModuleProperties()
      if (properties.isEmpty()) return@getOrComputeIfAbsent null

      val result = { instance: Any ->
        properties.flatMap { property ->
          val returnType = property.returnType
          when {
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
        }
      }

      result
    }
  }

  override fun beforeEach(context: ExtensionContext) {
    val instance = context.requiredTestInstance
    val modulesStore = context.getStore(getClassLevelNamespace(context))

    @Suppress("UNCHECKED_CAST")
    val providedModulesSupplier = modulesStore.get(context.requiredTestClass) as ((Any) -> List<Module>)?
      ?: return

    val providedModules = providedModulesSupplier(instance)

    val modules = BASE_MODULES + providedModules

    val injectorStore = context.getStore(getInjectorNamespace(context))

    val injector = injectorStore.getOrComputeIfAbsent(context.requiredTestMethod) {
      Guice.createInjector(Stage.DEVELOPMENT, modules)
    } as Injector

    for (lifecycleAction in injector.getInstance<Set<TestLifecycleAction>>()) {
      lifecycleAction.beforeEach()
    }

    injector.injectMembers(instance)
  }

  override fun afterEach(context: ExtensionContext) {
    val injectorStore = context.getStore(getInjectorNamespace(context))
    val injector = injectorStore.get(context.requiredTestMethod) as? Injector
      ?: return

    for (lifecycleAction in injector.getInstance<Set<TestLifecycleAction>>()) {
      lifecycleAction.afterEach()
    }

    injectorStore.remove(context.requiredTestMethod)
  }

  private fun getClassLevelNamespace(context: ExtensionContext): Namespace {
    return Namespace.create(GuiceExtension::class, context.requiredTestClass, "modules")
  }

  private fun getInjectorNamespace(context: ExtensionContext): Namespace {
    return Namespace.create(GuiceExtension::class, context.requiredTestClass, context.requiredTestInstance, "injectors")
  }

  companion object {
    private val BASE_MODULES = listOf<Module>(
      TestModule,
    )
  }

  interface TestLifecycleAction {
    fun beforeEach(): Unit = Unit
    fun afterEach(): Unit = Unit
  }

  private object TestModule : KAbstractModule() {
    override fun configure() {
      bind<Sha256FileSystemFactory>().toProvider(Provider { Sha256FileSystemFactory() })

      bindSet<TestLifecycleAction> { }
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
