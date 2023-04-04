package net.navatwo.kaff4

import org.assertj.core.api.AbstractMapAssert

fun <SELF : AbstractMapAssert<SELF, ACTUAL, K, V>, ACTUAL : Map<K, V>, K, V> SELF.containsExactlyInAnyOrderEntriesOf(
  vararg entries: Pair<K, V>,
): SELF {
  return containsExactlyInAnyOrderEntriesOf(entries.toMap())
}

fun <SELF : AbstractMapAssert<SELF, ACTUAL, K, V>, ACTUAL : Map<K, V>, K, V> SELF.containsAllEntriesOf(
  vararg entries: Pair<K, V>,
): SELF {
  return containsAllEntriesOf(entries.toMap())
}
