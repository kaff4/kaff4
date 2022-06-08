package com.github.nava2.aff4

import com.github.nava2.guice.KAbstractModule
import com.google.inject.Provides
import java.security.SecureRandom
import java.util.Random

object RandomsModule : KAbstractModule() {
  override fun configure() = Unit

  @Provides
  fun providesRandom(): Random = SecureRandom.getInstanceStrong()
}
