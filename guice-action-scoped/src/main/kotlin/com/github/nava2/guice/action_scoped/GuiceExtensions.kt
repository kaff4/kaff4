package com.github.nava2.guice.action_scoped

import com.google.inject.Key
import com.google.inject.TypeLiteral

internal inline fun <reified T> typeLiteral(): TypeLiteral<T> = object : TypeLiteral<T>() {}

internal inline fun <reified T> key(): Key<T> = Key.get(typeLiteral<T>())
