package net.navatwo.kaff4

import com.google.inject.Provides
import net.navatwo.guice.KAff4AbstractModule
import java.security.SecureRandom
import java.util.Random

object RandomsModule : KAff4AbstractModule() {
  override fun configure() {
    binder().requireAtInjectOnConstructors()
  }

  @Provides
  fun providesRandom(): Random = SecureRandom.getInstanceStrong()
}
