package com.github.nava2.aff4.meta.parser

import com.github.nava2.aff4.meta.Namespaces
import javax.inject.Singleton

@Singleton
internal class NamespacesContainer {
  var namespaces: Namespaces = Namespaces(mapOf())
}