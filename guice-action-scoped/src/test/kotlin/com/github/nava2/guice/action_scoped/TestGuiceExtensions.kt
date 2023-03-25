package com.github.nava2.guice.action_scoped

import com.google.inject.Injector
import javax.inject.Provider

internal inline fun <reified T : Any> Injector.getInstance(): T {
  return getInstance(key<T>())
}

internal inline fun <reified T : Any> Injector.getProvider(): Provider<T> {
  return getProvider(key<T>())
}
