// The MIT License (MIT)
//
// Copyright (c) 2016 Mason M Lai
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
// Original: https://github.com/charcuterie/interval-tree/blob/65dc2fc8f754127aa09fba0dff6f43b10ac151cb/src/datastructures/Interval.java
package com.github.nava2.aff4.interval_tree

/**
 * Closed-open, [), interval on the integer number line.
 */
interface Interval : Comparable<Interval>, ClosedRange<Long> {
  /**
   * Returns the ending point of `this`.
   *
   * The interval does **not** include this point.
   */
  val endExclusive: Long

  override val endInclusive: Long
    get() = endExclusive - 1

  /**
   * Returns the length of this.
   */
  val length: Long
    get() = endExclusive - start

  /**
   * Returns if this interval is adjacent to the specified interval.
   *
   *
   * Two intervals are adjacent if either one ends where the other starts.
   * @param other - the interval to compare this one to
   * @return if this interval is adjacent to the specified interval.
   */
  fun isAdjacent(other: Interval): Boolean {
    return start == other.endExclusive || endExclusive == other.start
  }

  fun overlaps(o: Interval): Boolean {
    return endExclusive > o.start && o.endExclusive > start
  }

  fun isAdjacentOrOverlaps(o: Interval): Boolean {
    return endExclusive >= o.start && o.endExclusive >= start
  }

  fun merge(other: Interval): Simple {
    require(isAdjacentOrOverlaps(other)) { "$this can not merge with $other due to no overlapping or adjacent parts" }
    val newStart = minOf(start, other.start)
    val newEnd = maxOf(endExclusive, other.endExclusive)
    return Simple(newStart, newEnd - newStart)
  }

  override operator fun compareTo(other: Interval): Int {
    return if (start > other.start) {
      1
    } else if (start < other.start) {
      -1
    } else if (endExclusive > other.endExclusive) {
      1
    } else if (endExclusive < other.endExclusive) {
      -1
    } else {
      0
    }
  }

  data class Simple(override val start: Long, override val length: Long) : Interval {
    override val endInclusive: Long = start + length - 1
    override val endExclusive: Long = start + length
  }
}
