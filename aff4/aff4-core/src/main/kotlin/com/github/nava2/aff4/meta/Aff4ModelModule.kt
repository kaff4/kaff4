package com.github.nava2.aff4.meta

import com.github.nava2.guice.KAbstractModule

object Aff4ModelModule : KAbstractModule() {
  override fun configure() {
    bindSet<Aff4Model.Factory> {
      to<BlockHashes.Factory>()
    }
  }
}
