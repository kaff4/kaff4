package net.navatwo.test

import net.navatwo.kaff4.UsingTemporary
import net.navatwo.kaff4.io.Sha256FileSystemFactory
import net.navatwo.kaff4.io.relativeTo
import okio.FileSystem
import okio.Path
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import java.security.SecureRandom
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

private const val KEY_PROPERTY_TEMP_DIRS = "temp-dirs/children"
private const val KEY_ROOT_TEMP_DIR = "temp-dirs/root"

class OkioTempFileSystemExtension : BeforeEachCallback, AfterEachCallback {

  private val systemFileSystem = FileSystem.SYSTEM
  private val random = SecureRandom()

  private fun getStoreNamespace(context: ExtensionContext): Namespace {
    return Namespace.create(context.requiredTestClass, context.requiredTestInstance, context.requiredTestMethod)
  }

  override fun beforeEach(context: ExtensionContext) {
    val testInstance = context.testInstance.orElse(null) ?: return

    val temporaryProperties = context.requiredTestClass.kotlin.memberProperties
      .filter { it.hasAnnotation<UsingTemporary>() }
      .filterIsInstance<KMutableProperty1<Any, FileSystem>>()

    if (temporaryProperties.isEmpty()) return

    val store = context.getStore(getStoreNamespace(context))

    val tempPath = store.getOrComputeIfAbsent(KEY_ROOT_TEMP_DIR) {
      val className = context.requiredTestClass.simpleName.replace(" ", "_")
      val methodName = context.requiredTestMethod.name.replace(" ", "_")
      val directory = "junit-$className-$methodName-${random.nextLong(Long.MAX_VALUE).toString(radix = 16)}"
      val tempPath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / directory

      systemFileSystem.createDirectories(tempPath, mustCreate = true)

      tempPath
    } as Path

    val temporaryDirectories = temporaryProperties.map { property ->
      property.isAccessible = true

      val propertyPath = tempPath / property.name
      systemFileSystem.createDirectories(propertyPath)

      val useSha256 = property.findAnnotation<UsingTemporary>()!!.useSha256
      val injectedFileSystem = if (useSha256) {
        Sha256FileSystemFactory().create(systemFileSystem, propertyPath)
      } else {
        systemFileSystem.relativeTo(propertyPath)
      }

      property.set(testInstance, injectedFileSystem)

      propertyPath
    }

    store.put(KEY_PROPERTY_TEMP_DIRS, temporaryDirectories)
  }

  override fun afterEach(context: ExtensionContext) {
    val store = context.getStore(getStoreNamespace(context))

    val rootDir = store.get(KEY_ROOT_TEMP_DIR, Path::class.java) ?: return

    @Suppress("UNCHECKED_CAST")
    val tempDirectories = store.get(KEY_PROPERTY_TEMP_DIRS, List::class.java) as? List<Path> ?: listOf()

    for (childDir in tempDirectories) {
      try {
        systemFileSystem.deleteRecursively(childDir)
      } catch (_: Exception) {
      }
    }

    try {
      systemFileSystem.delete(rootDir)
    } catch (_: Exception) {
    }
  }
}
