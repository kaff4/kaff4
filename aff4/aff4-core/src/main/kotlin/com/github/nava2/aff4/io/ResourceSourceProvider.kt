package com.github.nava2.aff4.io

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Source

class ResourceSourceProvider(
  resourceName: String,
) : SourceProvider<Source> by FileSystem.RESOURCES.sourceProvider(resourceName.toPath())