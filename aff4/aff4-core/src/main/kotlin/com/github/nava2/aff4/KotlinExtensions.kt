package com.github.nava2.aff4

suspend inline fun <T> SequenceScope<T>.yieldNotNull(nullableValue: T?) {
  if (nullableValue != null) {
    yield(nullableValue)
  }
}

inline fun <K, V, R> Map<K, V>.mapNotNullValues(transform: (Map.Entry<K, V>) -> R?): Map<K, R> {
  val resultMap = LinkedHashMap<K, R>()

  for (entry in this) {
    val mapped = transform(entry)
    if (mapped != null) {
      resultMap[entry.key] = mapped
    }
  }

  return resultMap
}
