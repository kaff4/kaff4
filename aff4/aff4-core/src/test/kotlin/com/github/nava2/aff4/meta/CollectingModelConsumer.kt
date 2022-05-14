package com.github.nava2.aff4.meta

import com.github.nava2.aff4.meta.rdf.model.Aff4RdfModel
import java.util.function.Consumer

class CollectingModelConsumer : Consumer<Aff4RdfModel> {
  private val mutableModels = mutableListOf<Aff4RdfModel>()

  val models: List<Aff4RdfModel>
    get() = mutableModels

  override fun accept(t: Aff4RdfModel) {
    mutableModels.add(t)
  }
}
