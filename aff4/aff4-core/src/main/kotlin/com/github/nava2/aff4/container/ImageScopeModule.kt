package com.github.nava2.aff4.container

import com.github.nava2.guice.KAff4AbstractModule
import com.github.nava2.guice.action_scoped.ActionScope
import com.github.nava2.guice.action_scoped.ActionScopeModule
import com.github.nava2.guice.action_scoped.ActionScoped
import com.github.nava2.guice.key
import javax.inject.Provider

object ImageScopeModule : KAff4AbstractModule() {
  override fun configure() {
    install(ActionScopeModule)

    bind(key<RealAff4ImageOpener.LoadedContainersContext>())
      .toProvider(seededKeyProvider())
      .`in`(ActionScoped::class.java)
  }

  /**
   * Returns a provider that always throws exception complaining that the object
   * in question must be seeded before it can be injected.
   *
   * @return typed provider
   */
  private inline fun <reified T> seededKeyProvider(): Provider<T> {
    return Provider<T> {
      error(
        buildString {
          append("If you got here then it means that you've tried to use an Image type ")
          append(" without opening an action scope and providing it as a seeded value.")
          append(" Your code asked for scoped ${T::class.qualifiedName} which should have been")
          append(" explicitly seeded in this scope by calling")
          append(" ${ActionScope::class.simpleName}.start(mapOf(...)), but was not.")
        }
      )
    }
  }
}
