@file:InternalApi

package net.navatwo.guice

import com.google.inject.Module
import com.google.inject.assistedinject.FactoryModuleBuilder
import net.navatwo.kaff4.api.InternalApi

inline fun <reified F> assistedFactoryModule(setupFn: FactoryModuleBuilder.() -> Unit = {}): Module {
  return FactoryModuleBuilder()
    .apply(setupFn)
    .build(F::class.java)
}

inline fun <reified T, reified U : T> FactoryModuleBuilder.implement(): FactoryModuleBuilder {
  return implement(T::class.java, U::class.java)
}

inline fun <reified T> FactoryModuleBuilder.build(): Module {
  return build(T::class.java)
}
