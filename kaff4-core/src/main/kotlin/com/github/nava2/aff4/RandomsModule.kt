package com.github.nava2.aff4

import com.github.nava2.guice.KAff4AbstractModule
import com.google.inject.Provides
import java.security.SecureRandom
import java.util.Random

object RandomsModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()
  }

  @Provides
  fun providesRandom(): Random = SecureRandom.getInstanceStrong()
}
