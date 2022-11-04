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
// Original: https://github.com/charcuterie/interval-tree/blob/65dc2fc8f754127aa09fba0dff6f43b10ac151cb/src/testing/IntervalTreeTest.java

@file:Suppress(
  "LargeClass",
)

package com.github.nava2.aff4.interval_tree

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random
import java.util.TreeSet
import java.util.stream.StreamSupport

class IntervalTreeTest {
  // an empty tree
  private val emptyTree = IntervalTree<Impl>()

  private val singletonValue = Impl(0, 10) // [0, 10)

  // a tree with one node:
  private val singletonTree = IntervalTree(singletonValue)

  private val copyOfSingletonValue = Impl(singletonValue)
  private val randomTree = IntervalTree<Impl>()
  private val randomIntervals = TreeSet<Impl>()

  private val randomUpperBound = 3000L
  private val numRandomIntervals = 5000

  // A tree with a dead-zone in the middle to test overlap methods in each section
  private val gappedTree = IntervalTree<Impl>()
  private val gappedUpperBound = 3000L
  private val gappedLowerBound = 4000L
  private val numGappedIntervals = 2500

  private val gappedIntervals = mutableSetOf<Impl>()

  @BeforeEach
  fun initRandom() {
    val rand = Random()
    for (i in generateIntervals(numRandomIntervals, randomUpperBound, rand = rand)) {
      randomIntervals.add(i)
      randomTree.insert(i)
    }

    for (i in generateIntervals(numGappedIntervals, gappedUpperBound, rand = rand)) {
      gappedIntervals.add(i)
      gappedTree.insert(i)
    }

    for (i in generateIntervals(numGappedIntervals, gappedUpperBound, lowerBound = gappedLowerBound, rand = rand)) {
      gappedIntervals.add(i)
      gappedTree.insert(i)
    }
  }

  // ////////////////////
  // Empty tree tests //
  // ////////////////////

  @Test
  fun testEmptyTreeSize() {
    assertThat(emptyTree).isEmpty()
    assertThat(emptyTree).hasSize(0)
    assertThat(emptyTree).isEmpty()
  }

  @Test
  fun testEmptyTreeContains() {
    assertThat(emptyTree).doesNotContain(Impl(1, 5))
  }

  @Test
  fun testEmptyTreeMinimum() {
    assertThat(emptyTree.minimum()).isNotPresent()
  }

  @Test
  fun testEmptyTreeMaximum() {
    assertThat(emptyTree.maximum()).isNotPresent()
  }

  @Test
  fun testEmptyTreeSuccessor() {
    assertThat(emptyTree.successor(Impl(1, 2))).isNotPresent()
  }

  @Test
  fun testEmptyTreePredecessor() {
    assertThat(emptyTree.predecessor(Impl(1, 2))).isNotPresent()
  }

  @Test
  fun testEmptyTreeIteratorHasNext() {
    assertThat(emptyTree.iterator().hasNext()).isFalse()
  }

  @Test
  fun testEmptyTreeIteratorNext() {
    assertThatThrownBy { emptyTree.iterator().next() }
      .isInstanceOf(NoSuchElementException::class.java)
      .hasMessage("Interval tree has no more elements.")
  }

  @Test
  fun testEmptyTreeOverlaps() {
    assertThat(emptyTree.overlaps(Impl(1, 10))).isFalse()
  }

  @Test
  fun testEmptyTreeOverlappersHasNext() {
    assertThat(emptyTree.overlappers(Impl(1, 3)).hasNext()).isFalse()
  }

  @Test
  fun testEmptyTreeOverlappersNext() {
    assertThatThrownBy { emptyTree.overlappers(Impl(1, 3)).next() }
      .isInstanceOf(NoSuchElementException::class.java)
      .hasMessage("Interval tree has no more overlapping elements.")
  }

  @Test
  fun testEmptyTreeNumOverlappers() {
    assertThat(emptyTree.numOverlappers(Impl(1, 3))).isZero()
  }

  @Test
  fun testEmptyTreeIsValidBST() {
    assertThat(emptyTree).isEmpty()
  }

  @Test
  fun testEmptyTreeIsBalanced() {
    assertThat(emptyTree.isBalanced()).isTrue()
  }

  @Test
  fun testEmptyTreeHasValidRedColoring() {
    assertThat(emptyTree.hasValidRedColoring()).isTrue()
  }

  @Test
  fun testEmptyTreeConsistentMaxEnds() {
    assertThat(emptyTree.hasConsistentMaxEnds()).isTrue()
  }

  @Test
  fun testEmptyTreeDelete() {
    assertThat(emptyTree.delete(Impl(1, 2))).isFalse()
  }

  @Test
  fun testEmptyTreeSizeAfterDelete() {
    emptyTree.delete(Impl(1, 2))
    assertThat(emptyTree).isEmpty()
  }

  @Test
  fun testEmptyTreeIsEmptyAfterDelete() {
    emptyTree.delete(Impl(1, 2))
    assertThat(emptyTree).isEmpty()
  }

  @Test
  fun testEmptyTreeDeleteMin() {
    assertThat(emptyTree.deleteMin()).isFalse()
  }

  @Test
  fun testEmptyTreeSizeAfterDeleteMin() {
    emptyTree.deleteMin()
    assertThat(emptyTree).isEmpty()
  }

  @Test
  fun testEmptyTreeIsEmptyAfterDeleteMin() {
    emptyTree.deleteMin()
    assertThat(emptyTree).isEmpty()
  }

  @Test
  fun testEmptyTreeDeleteMax() {
    assertThat(emptyTree.deleteMax()).isFalse()
  }

  @Test
  fun testEmptyTreeSizeAfterDeleteMax() {
    emptyTree.deleteMax()
    assertThat(emptyTree).isEmpty()
  }

  @Test
  fun testEmptyTreeIsEmptyAfterDeleteMax() {
    emptyTree.deleteMax()
    assertThat(emptyTree).isEmpty()
  }

  @Test
  fun testEmptyTreeDeleteOverlappers() {
    emptyTree.deleteOverlappers(Impl(1, 2))
  }

  @Test
  fun testEmptyTreeSizeAfterDeleteOverlappers() {
    emptyTree.deleteOverlappers(Impl(1, 2))
    assertThat(emptyTree).isEmpty()
  }

  @Test
  fun testEmptyTreeIsEmptyAfterDeleteOverlappers() {
    emptyTree.deleteOverlappers(Impl(1, 2))
    assertThat(emptyTree).isEmpty()
  }

  @Test
  fun testEmptyTreeIsValidBSTAfterDeletion() {
    emptyTree.delete(Impl(1, 3))
    assertThat(emptyTree.isBST()).isTrue()
  }

  @Test
  fun testEmptyTreeIsBalancedAfterDeletion() {
    emptyTree.delete(Impl(1, 3))
    assertThat(emptyTree.isBalanced()).isTrue()
  }

  @Test
  fun testEmptyTreeHasValidRedColoringAfterDeletion() {
    emptyTree.delete(Impl(1, 3))
    assertThat(emptyTree.hasValidRedColoring()).isTrue()
  }

  @Test
  fun testEmptyTreeConsistentMaxEndsAfterDeletion() {
    emptyTree.delete(Impl(1, 3))
    assertThat(emptyTree.hasConsistentMaxEnds()).isTrue()
  }

  @Test
  fun testEmptyTreeInsertion() {
    assertThat(emptyTree.insert(Impl(1, 3))).isTrue()
  }

  @Test
  fun testEmptyTreeSizeAfterInsertion() {
    emptyTree.insert(Impl(1, 2))
    assertThat(emptyTree).hasSize(1)
  }

  @Test
  fun testEmptyTreeIsEmptyAfterInsertion() {
    emptyTree.insert(Impl(1, 2))
    assertThat(emptyTree.isEmpty()).isFalse()
  }

  @Test
  fun testEmptyTreeIsValidBSTAfterInsertion() {
    emptyTree.insert(Impl(1, 3))
    assertThat(emptyTree.isBST()).isTrue()
  }

  @Test
  fun testEmptyTreeIsBalancedAfterInsertion() {
    emptyTree.insert(Impl(1, 3))
    assertThat(emptyTree.isBalanced()).isTrue()
  }

  @Test
  fun testEmptyTreeHasValidRedColoringAfterInsertion() {
    emptyTree.insert(Impl(1, 3))
    assertThat(emptyTree.hasValidRedColoring()).isTrue()
  }

  @Test
  fun testEmptyTreeHasConsistentMaxEndsAfterInsertion() {
    emptyTree.insert(Impl(1, 3))
    assertThat(emptyTree.hasConsistentMaxEnds()).isTrue()
  }

  @Test
  fun testEmptyTreeIsValidBSTAfterRepeatedInsertions() {
    for (i in generateIntervals(numRandomIntervals, randomUpperBound)) {
      emptyTree.insert(i)
      assertThat(emptyTree.isBST()).isTrue()
    }
  }

  @Test
  fun testEmptyTreeIsBalancedAfterRepeatedInsertions() {
    for (i in generateIntervals(numRandomIntervals, randomUpperBound)) {
      emptyTree.insert(i)
      assertThat(emptyTree.isBalanced()).isTrue()
    }
  }

  @Test
  fun testEmptyTreeHasValidRedColoringAfterRepeatedInsertions() {
    for (i in generateIntervals(numRandomIntervals, randomUpperBound)) {
      emptyTree.insert(i)
      assertThat(emptyTree.hasValidRedColoring()).isTrue()
    }
  }

  @Test
  fun testEmptyTreeHasConsistentMaxEndsAfterRepeatedInsertions() {
    for (i in generateIntervals(numRandomIntervals, randomUpperBound)) {
      emptyTree.insert(i)
      assertThat(emptyTree.hasConsistentMaxEnds()).isTrue()
    }
  }

  // ////////////////////////
  // Singleton tree tests //
  // ////////////////////////
  @Test
  fun testSingletonTreeIsEmpty() {
    assertThat(singletonTree.isEmpty()).isFalse()
  }

  @Test
  fun testSingletonTreeSize() {
    assertThat(singletonTree).hasSize(1)
  }

  @Test
  fun testSingletonTreeContainsPositive() {
    assertThat(singletonTree.contains(copyOfSingletonValue)).isTrue()
  }

  @Test
  fun testSingletonTreeContainsNegative() {
    assertThat(singletonTree.contains(Impl(1, 9))).isFalse()
  }

  @Test
  fun testSingletonTreeMinimum() {
    assertThat(singletonTree.minimum()).contains(copyOfSingletonValue)
  }

  @Test
  fun testSingletonTreeMaximum() {
    assertThat(singletonTree.maximum()).contains(copyOfSingletonValue)
  }

  @Test
  fun testSingletonTreeSuccessor() {
    assertThat(singletonTree.successor(copyOfSingletonValue)).isNotPresent()
  }

  @Test
  fun testSingetonTreePredecessor() {
    assertThat(singletonTree.predecessor(copyOfSingletonValue)).isNotPresent()
  }

  @Test
  fun testSingletonTreeIteratorHasNext() {
    assertThat(singletonTree.iterator()).hasNext()
  }

  @Test
  fun testSingletonTreeIteratorNext() {
    assertThat(singletonTree.iterator().next()).isEqualTo(copyOfSingletonValue)
  }

  @Test
  fun testSingletonTreeIteratorNextTwice() {
    assertThatThrownBy {
      val i: Iterator<Impl> = singletonTree.iterator()

      i.next()
      i.next()
    }.isInstanceOf(NoSuchElementException::class.java)
      .hasMessage("Interval tree has no more elements.")
  }

  @Test
  fun testSingletonTreeOverlapsPositive() {
    assertThat(singletonTree.overlaps(copyOfSingletonValue)).isTrue()
  }

  @Test
  fun testSingletonTreeOverlapsNegative() {
    assertThat(singletonTree.overlaps(Impl(20, 22))).isFalse()
  }

  @Test
  fun testSingletonTreeOverlapsAdjacent() {
    assertThat(singletonTree.overlaps(Impl(10, 20))).isFalse()
  }

  @Test
  fun testSingletonTreeOverlappersHasNext() {
    assertThat(singletonTree.overlappers(Impl(1, 3)).hasNext()).isTrue()
  }

  @Test
  fun testSingletonTreeOverlappersNext() {
    assertThat(singletonTree.overlappers(Impl(1, 3)).next()).isEqualTo(copyOfSingletonValue)
  }

  @Test
  fun testSingletonTreeOverlappersNextTwice() {
    assertThatThrownBy {
      val i = singletonTree.overlappers(Impl(1, 3))
      i.next()
      i.next()
    }.isInstanceOf(NoSuchElementException::class.java)
      .hasMessage("Interval tree has no more overlapping elements.")
  }

  @Test
  fun testSingletonTreeNumOverlappers() {
    assertThat(singletonTree.numOverlappers(Impl(1, 3))).isEqualTo(1)
  }

  @Test
  fun testSingletonTreeIsValidBST() {
    assertThat(singletonTree.isBST()).isTrue()
  }

  @Test
  fun testSingletonTreeIsBalanced() {
    assertThat(singletonTree.isBalanced()).isTrue()
  }

  @Test
  fun testSingletonTreeHasValidRedColoring() {
    assertThat(singletonTree.hasValidRedColoring()).isTrue()
  }

  @Test
  fun testSingletonTreeConsistentMaxEnds() {
    assertThat(singletonTree.hasConsistentMaxEnds()).isTrue()
  }

  @Test
  fun testSingletonTreeDeletePositive() {
    assertThat(singletonTree.delete(copyOfSingletonValue)).isTrue()
  }

  @Test
  fun testSingletonTreeDeleteNegative() {
    assertThat(singletonTree.delete(Impl(1, 5))).isFalse()
  }

  @Test
  fun testSingletonTreeSizeAfterSuccessfulDeletion() {
    singletonTree.delete(copyOfSingletonValue)
    assertThat(singletonTree).isEmpty()
  }

  @Test
  fun testSingletonTreeSizeAfterUnsuccessfulDeletion() {
    singletonTree.delete(Impl(1, 9))
    assertThat(singletonTree).hasSize(1)
  }

  @Test
  fun testSingletonTreeIsEmptyAfterSuccessfulDeletion() {
    singletonTree.delete(copyOfSingletonValue)
    assertThat(singletonTree).isEmpty()
  }

  @Test
  fun testSingletonTreeIsEmptyAfterUnsuccessfulDeletion() {
    singletonTree.delete(Impl(1, 9))
    assertThat(singletonTree.isEmpty()).isFalse()
  }

  @Test
  fun testSingletonTreeDeleteMin() {
    assertThat(singletonTree.deleteMin()).isTrue()
  }

  @Test
  fun testSingletonTreeSizeAfterDeleteMin() {
    singletonTree.deleteMin()
    assertThat(singletonTree).isEmpty()
  }

  @Test
  fun testSingletonTreeIsEmptyAfterDeleteMin() {
    singletonTree.deleteMin()
    assertThat(singletonTree).isEmpty()
  }

  @Test
  fun testSingletonTreeDeleteMax() {
    assertThat(singletonTree.deleteMax()).isTrue()
  }

  @Test
  fun testSingletonTreeSizeAfterDeleteMax() {
    singletonTree.deleteMax()
    assertThat(singletonTree).isEmpty()
  }

  @Test
  fun testSingletonTreeIsEmptyAfterDeleteMax() {
    singletonTree.deleteMax()
    assertThat(singletonTree).isEmpty()
  }

  @Test
  fun testSingletonTreeDeleteOverlappers() {
    assertThat(singletonTree.deleteOverlappers(Impl(1, 5))).isTrue()
  }

  @Test
  fun testSingletonTreeSizeAfterDeleteOverlappersPositive() {
    singletonTree.deleteOverlappers(Impl(1, 5))
    assertThat(singletonTree).isEmpty()
  }

  @Test
  fun testSingletonTreeSizeAfterDeleteOverlappersNegative() {
    singletonTree.deleteOverlappers(Impl(20, 25))
    assertThat(singletonTree).hasSize(1)
  }

  @Test
  fun testSingletonTreeIsEmptyAfterDeleteOverlappers() {
    singletonTree.deleteOverlappers(Impl(1, 5))
    assertThat(singletonTree).isEmpty()
  }

  @Test
  fun testSingletonTreeIsNotEmptyAfterDeleteOverlappers() {
    singletonTree.deleteOverlappers(Impl(20, 25))
    assertThat(singletonTree.isEmpty()).isFalse()
  }

  @Test
  fun testSingletonTreeIsValidBSTAfterDeletion() {
    singletonTree.delete(copyOfSingletonValue)
    assertThat(singletonTree.isBST()).isTrue()
  }

  @Test
  fun testSingletonTreeIsBalancedAfterDeletion() {
    singletonTree.delete(copyOfSingletonValue)
    assertThat(singletonTree.isBalanced()).isTrue()
  }

  @Test
  fun testSingletonTreeHasValidRedColoringAfterDeletion() {
    singletonTree.delete(copyOfSingletonValue)
    assertThat(singletonTree.hasValidRedColoring()).isTrue()
  }

  @Test
  fun testSingletonTreeConsistentMaxEndsAfterDeletion() {
    singletonTree.delete(copyOfSingletonValue)
    assertThat(singletonTree.hasConsistentMaxEnds()).isTrue()
  }

  @Test
  fun testSingletonTreeInsertion() {
    assertThat(singletonTree.insert(Impl(1, 11))).isTrue()
  }

  @Test
  fun testSingletonTreeRedundantInsertion() {
    assertThat(singletonTree.insert(copyOfSingletonValue)).isFalse()
  }

  @Test
  fun testSingletonTreeSizeAfterInsertion() {
    singletonTree.insert(Impl(1, 2))
    assertThat(singletonTree).hasSize(2)
  }

  @Test
  fun testSingletonTreeSizeAfterRedundantInsertion() {
    singletonTree.insert(copyOfSingletonValue)
    assertThat(singletonTree).hasSize(1)
  }

  @Test
  fun testSingletonTreeIsNotEmptyAfterInsertion() {
    singletonTree.insert(Impl(1, 2))
    assertThat(singletonTree.isEmpty()).isFalse()
  }

  @Test
  fun testSingletonTreeIsValidBSTAfterInsertion() {
    singletonTree.insert(Impl(1, 3))
    assertThat(singletonTree.isBST()).isTrue()
  }

  @Test
  fun testSingletonTreeIsValidBSTAfterRedundantInsertion() {
    singletonTree.insert(copyOfSingletonValue)
    assertThat(singletonTree.isBST()).isTrue()
  }

  @Test
  fun testSingletonTreeIsBalancedAfterInsertion() {
    singletonTree.insert(Impl(1, 3))
    assertThat(singletonTree.isBalanced()).isTrue()
  }

  @Test
  fun testSingletonTreeIsBalancedAfterRedundantInsertion() {
    singletonTree.insert(copyOfSingletonValue)
    assertThat(singletonTree.isBalanced()).isTrue()
  }

  @Test
  fun testSingletonTreeHasValidRedColoringAfterInsertion() {
    singletonTree.insert(Impl(1, 3))
    assertThat(singletonTree.hasValidRedColoring()).isTrue()
  }

  @Test
  fun testSingletonTreeHasValidRedColoringAfterRedundantInsertion() {
    singletonTree.insert(copyOfSingletonValue)
    assertThat(singletonTree.hasValidRedColoring()).isTrue()
  }

  @Test
  fun testSingletonTreeConsistentMaxEndsAfterInsertion() {
    singletonTree.insert(Impl(1, 3))
    assertThat(singletonTree.hasConsistentMaxEnds()).isTrue()
  }

  @Test
  fun testSingletonTreeConsistentMaxEndsAfterRedundantInsertion() {
    singletonTree.insert(copyOfSingletonValue)
    assertThat(singletonTree.hasConsistentMaxEnds()).isTrue()
  }

  // /////////////////////
  // Random tree tests //
  // /////////////////////
  @Test
  fun testRandomTreeIsNotEmpty() {
    assertThat(randomTree.isEmpty()).isFalse()
  }

  @Test
  fun testRandomTreeSize() {
    assertThat(randomTree).hasSize(randomIntervals.size)
  }

  @Test
  fun testRandomTreeContainsPositive() {
    randomTree.insert(Impl(1000, 2000))
    assertThat(randomTree.contains(Impl(1000, 2000))).isTrue()
  }

  @Test
  fun testRandomTreeMinimum() {
    val i = randomIntervals.iterator().next()
    assertThat(randomTree.minimum()).contains(i)
  }

  @Test
  fun testRandomTreeMaximum() {
    val iter = randomIntervals.iterator()
    var i: Impl? = null
    while (iter.hasNext()) {
      i = iter.next()
    }
    assertThat(randomTree.maximum()).contains(i)
  }

  @Test
  fun testRandomTreePredecessorOfMinimum() {
    assertThat(randomTree.minimum().flatMap { t -> randomTree.predecessor(t) }).isNotPresent()
  }

  @Test
  fun testRandomTreeSuccessorOfMinimum() {
    val successor = randomTree.minimum().flatMap { t -> randomTree.successor(t) }
    val iter = randomIntervals.iterator()
    iter.next()
    assertThat(successor).contains(iter.next())
  }

  @Test
  fun testRandomTreeSuccessorOfMaximum() {
    assertThat(
      randomTree.maximum()
        .flatMap { t -> randomTree.successor(t) }
    ).isNotPresent()
  }

  @Test
  fun testRandomTreePredecessorOfMaximum() {
    val predecessor = randomTree.maximum().flatMap { t -> randomTree.predecessor(t) }
    val iter = randomIntervals.iterator()
    var prev = iter.next()
    var curr = iter.next()
    while (iter.hasNext()) {
      prev = curr
      curr = iter.next()
    }
    assertThat(predecessor).contains(prev)
  }

  @Test
  fun testRandomTreeIteratorNumberOfElements() {
    val count = StreamSupport.stream(randomTree.spliterator(), false).count()
    assertThat(randomIntervals).hasSize(count.toInt())
  }

  @Test
  fun testRandomTreeOverlapsPositive() {
    val cmp = Impl(1000, 2000) // Not guaranteed to overlap,
    // but unlikely not to
    assertThat(randomTree.overlaps(cmp)).isTrue()
  }

  @Test
  fun testRandomTreeOverlapsNegative1() {
    val cmp = Impl(randomUpperBound, randomUpperBound + 1000)
    assertThat(randomTree.overlaps(cmp)).isFalse()
  }

  @Test
  fun testRandomTreeOverlapsNegative2() {
    val cmp = Impl(-1000, 0)
    assertThat(randomTree.overlaps(cmp)).isFalse()
  }

  @Test
  fun testRandomTreeMinOverlapperPositive() {
    val cmp = Impl(1000, 2000)
    val setMin = randomIntervals.first { n -> n.overlaps(cmp) }
    val treeMin = randomTree.minimumOverlapper(cmp)
    assertThat(treeMin).contains(setMin)
  }

  @Test
  fun testRandomTreeMinOverlapperNegative() {
    val cmp = Impl(-1000, 0)
    assertThat(randomTree.minimumOverlapper(cmp)).isNotPresent()
  }

  @Test
  fun testRandomTreeNumOverlappers() {
    val i = Impl(1000, 2000)
    val count = randomTree.count { it.overlaps(i) }.toLong()
    assertThat(randomTree.numOverlappers(i)).isEqualTo(count)
  }

  @Test
  fun testRandomTreeSizeAfterDeleteOverlappers() {
    val i = Impl(1000, 2000)
    val initSize = randomTree.size
    val count = randomTree.count { it.overlaps(i) }
    randomTree.deleteOverlappers(i)
    assertThat(randomTree).hasSize(initSize - count)
  }

  @Test
  fun testRandomTreeNoOverlappersAfterDeleteOverlappers() {
    val i = Impl(1000, 2000)
    assertThat(randomTree.overlaps(i)).isTrue()
    randomTree.deleteOverlappers(i)
    assertThat(randomTree.overlaps(i)).isFalse()
    for (j in randomTree) {
      assertThat(j.overlaps(i)).isFalse()
    }
  }

  @Test
  fun testRandomTreeSizeAfterRepeatedDeletions() {
    val randomIntervalList = randomIntervals.shuffled()

    var count = randomIntervalList.size
    for (i in randomIntervalList) {
      randomTree.delete(i)
      count--

      assertThat(randomTree).hasSize(count)
      assertThat(randomTree).doesNotContain(i)
    }

    assertThat(randomTree).isEmpty()
  }

  @Test
  fun testRandomTreeIsValidBSTAfterRepeatedDeletions() {
    val randomIntervalList = randomIntervals.shuffled()
    for (i in randomIntervalList) {
      randomTree.delete(i)
      assertThat(randomTree.isBST()).isTrue()
    }
  }

  @Test
  fun testRandomTreeIsBalancedAfterRepeatedDeletions() {
    val randomIntervalList = randomIntervals.shuffled()
    for (i in randomIntervalList) {
      randomTree.delete(i)
      assertThat(randomTree.isBalanced()).isTrue()
    }
  }

  @Test
  fun testRandomTreeHasValidRedColoringAfterRepeatedDeletions() {
    val randomIntervalList = randomIntervals.shuffled()
    for (i in randomIntervalList) {
      randomTree.delete(i)
      assertThat(randomTree.hasValidRedColoring()).isTrue()
    }
  }

  @Test
  fun testRandomTreeConsistentMaxEndsAfterRepeatedDeletions() {
    val randomIntervalList = randomIntervals.shuffled()
    for (i in randomIntervalList) {
      randomTree.delete(i)
      assertThat(randomTree.hasConsistentMaxEnds()).isTrue()
    }
  }

  // /////////////////////
  // Gapped tree tests //
  // /////////////////////

  @Test
  fun testGappedTreeOverlapsPositive() {
    assertThat(gappedTree.overlaps(Impl(0, gappedUpperBound))).isTrue()
    assertThat(gappedTree.overlaps(Impl(gappedLowerBound, gappedUpperBound + gappedLowerBound))).isTrue()
    assertThat(gappedTree.overlaps(Impl(0, gappedUpperBound + gappedLowerBound))).isTrue()
  }

  @Test
  fun testGappedTreeOverlapsNegative() {
    assertThat(gappedTree.overlaps(Impl(gappedUpperBound, gappedLowerBound))).isFalse()
  }

  @Test
  fun testGappedTreeDeleteOverlappersPositive() {
    val firstInterval = Impl(0, gappedUpperBound)
    val secondInterval = Impl(gappedLowerBound, gappedUpperBound + gappedLowerBound)
    val first = gappedTree.deleteOverlappers(firstInterval)
    val second = gappedTree.deleteOverlappers(secondInterval)
    assertThat(first).isTrue().isEqualTo(second)
  }

  @Test
  fun testGappedTreeDeleteOverlappersNegative() {
    val interval = Impl(gappedUpperBound, gappedLowerBound)
    assertThat(gappedTree.deleteOverlappers(interval)).isFalse()
  }

  private fun generateIntervals(
    intervals: Int,
    upperBound: Long,
    lowerBound: Long = 0L,
    rand: Random = Random(),
  ): Sequence<Impl> = sequence {
    repeat(intervals) {
      var r = 0L
      var s = 0L
      while (s <= r) {
        r = lowerBound + rand.nextLong(upperBound)
        s = lowerBound + rand.nextLong(upperBound)
      }

      yield(Impl(r, s))
    }
  }

  /**
   * Simple implementation of Interval for testing
   */
  private data class Impl(override val start: Long, override val endExclusive: Long) : Interval {

    constructor(i: Impl) : this(i.start, i.endExclusive)

    override val length = endExclusive - start

    override fun toString(): String {
      return "start: $start end: $endExclusive"
    }
  }
}
