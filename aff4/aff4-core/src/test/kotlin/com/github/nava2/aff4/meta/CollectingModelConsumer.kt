package com.github.nava2.aff4.meta

import java.util.function.Consumer

class CollectingModelConsumer : Consumer<Aff4Model> {
  private val mutableModels = mutableListOf<Aff4Model>()

  val models: List<Aff4Model>
    get() = mutableModels

  override fun accept(t: Aff4Model) {
    mutableModels.add(t)
  }
}