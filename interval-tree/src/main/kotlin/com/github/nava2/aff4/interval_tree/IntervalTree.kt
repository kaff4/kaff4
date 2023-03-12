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
// Original: https://github.com/charcuterie/interval-tree/blob/65dc2fc8f754127aa09fba0dff6f43b10ac151cb/src/datastructures/IntervalTree.java

@file:Suppress(
  "ComplexMethod",
  "NestedBlockDepth",
  "TooManyFunctions",
  "LargeClass",
  "LoopWithTooManyJumpStatements",
  "ReturnCount",
)

package com.github.nava2.aff4.interval_tree

import com.google.common.annotations.VisibleForTesting
import java.util.Optional

/**
 * A balanced binary-search tree keyed by Interval objects.
 *
 *
 * The underlying data-structure is a red-black tree largely implemented from
 * CLRS (Introduction to Algorithms, 2nd edition) with the interval-tree
 * extensions mentioned in section 14.3
 * @param <I> - the type of Interval this tree contains
 */

class IntervalTree<T : Interval> : Iterable<T> {
  // The root Node.
  private var root: Node

  // The sentinel Node to represent the absence of a node.
  private var nil: Node

  /**
   * The number of intervals stored in this IntervalTree.
   */
  var size: Int
    private set

  constructor() {
    nil = Node()
    root = nil
    size = 0
  }

  /**
   * Constructs an IntervalTree containing a single node corresponding to
   * the given interval.
   * @param t - the interval to add to the tree
   */
  constructor(t: T) {
    nil = Node()
    root = Node(t)
    root.blacken()
    size = 1
  }

  constructor(v0: T, vararg values: T) : this(v0) {
    insertAll(values.asIterable())
  }

  // /////////////////////////////////
  // Tree -- General query methods //
  // /////////////////////////////////

  /**
   * Whether this IntervalTree is empty or not.
   */
  fun isEmpty(): Boolean = root.isNil

  /**
   * The Node in this IntervalTree that contains the given Interval.
   *
   *
   * This method returns the nil Node if the Interval t cannot be found.
   * @param t - the Interval to search for.
   */
  private fun search(t: Interval): Node {
    return root.search(t)
  }

  /**
   * Whether or not this IntervalTree contains the given Interval.
   * @param t - the Interval to search for
   */
  operator fun contains(t: T): Boolean = !search(t).isNil

  /**
   * The minimum value in this IntervalTree
   * @return an Optional containing, if it exists, the minimum value in this
   * IntervalTree; otherwise (i.e., if this is empty), an empty Optional.
   */
  fun minimum(): Optional<T> {
    val n: Node = root.minimumNode()
    return if (n.isNil) Optional.empty() else Optional.of(n.interval!!)
  }

  /**
   * The maximum value in this IntervalTree
   * @return an Optional containing, if it exists, the maximum value in this
   * IntervalTree; otherwise (i.e., if this is empty), an empty Optional.
   */
  fun maximum(): Optional<T> {
    val n: Node = root.maximumNode()
    return if (n.isNil) Optional.empty() else Optional.of(n.interval!!)
  }

  /**
   * The next Interval in this IntervalTree
   * @param t - the Interval to search for
   * @return an Optional containing, if it exists, the next Interval in this
   * IntervalTree; otherwise (if t is the maximum Interval, or if this
   * IntervalTree does not contain t), an empty Optional.
   */
  fun successor(t: Interval): Optional<T> {
    var n = search(t)
    if (n.isNil) {
      return Optional.empty()
    }
    n = n.successor()
    return if (n.isNil) {
      Optional.empty()
    } else {
      Optional.ofNullable(n.interval)
    }
  }

  /** @see [successor] */
  fun successor(start: Long, length: Long): Optional<T> = successor(Interval.Simple(start, length))

  fun closestSuccessor(t: Interval): Optional<T> {
    val n = root.closestSuccessor(t)
    return if (n.isNil) {
      Optional.empty()
    } else {
      Optional.ofNullable(n.interval)
    }
  }

  /**
   * The previous Interval in this IntervalTree
   * @param t - the Interval to search for
   * @return an Optional containing, if it exists, the previous Interval in
   * this IntervalTree; otherwise (if t is the minimum Interval, or if this
   * IntervalTree does not contain t), an empty Optional.
   */
  fun predecessor(t: Interval): Optional<T> {
    var n = search(t)
    if (n.isNil) {
      return Optional.empty()
    }
    n = n.predecessor()
    return if (n.isNil) {
      Optional.empty()
    } else {
      Optional.of(n.interval!!)
    }
  }

  /** @see [successor] */
  fun predecessor(start: Long, length: Long): Optional<T> = predecessor(Interval.Simple(start, length))

  /**
   * An Iterator which traverses the tree in ascending order.
   */
  override fun iterator(): Iterator<T> = TreeIterator(root)

  /**
   * An Iterator over the Intervals in this IntervalTree that overlap the
   * given Interval
   * @param t - the overlapping Interval
   */
  fun overlappers(t: Interval): Iterator<T> = root.overlappers(t)

  /**
   * An Iterator over the Intervals in this IntervalTree that overlap the
   * given Interval
   */
  fun overlappers(start: Long, length: Long): Iterator<T> = overlappers(Interval.Simple(start, length))

  /**
   * Whether or not any of the Intervals in this IntervalTree overlap the given
   * Interval
   * @param t - the potentially overlapping Interval
   */
  fun overlaps(t: Interval): Boolean = !root.anyOverlappingNode(t).isNil

  /**
   * An Iterator over the Intervals in this IntervalTree that overlap the
   * given Interval
   */
  fun overlaps(start: Long, length: Long): Boolean = overlaps(Interval.Simple(start, length))

  /**
   * The number of Intervals in this IntervalTree that overlap the given
   * Interval
   * @param t - the overlapping Interval
   */
  fun numOverlappers(t: Interval): Long {
    return root.numOverlappingNodes(t)
  }

  /**
   * The least Interval in this IntervalTree that overlaps the given Interval
   * @param t - the overlapping Interval
   * @return an Optional containing, if it exists, the least Interval in this
   * IntervalTree that overlaps the given Interval; otherwise (i.e., if there
   * is no overlap), an empty Optional
   */
  fun minimumOverlapper(t: Interval): Optional<T> {
    val n: Node = root.minimumOverlappingNode(t)
    return if (n.isNil) Optional.empty() else Optional.of(n.interval!!)
  }
  // /////////////////////////////
  // Tree -- Insertion methods //
  // /////////////////////////////
  /**
   * Inserts the given value into the IntervalTree.
   *
   *
   * This method constructs a new Node containing the given value and places
   * it into the tree. If the value already exists within the tree, the tree
   * remains unchanged.
   * @param t - the value to place into the tree
   * @return if the value did not already exist, i.e., true if the tree was
   * changed, false if it was not
   */
  fun insert(t: T): Boolean {
    val z = Node(t)
    var y = nil
    var x = root

    // Traverse the tree down to a leaf.
    while (!x.isNil) {
      y = x
      x.maxEnd = x.maxEnd.coerceAtLeast(z.maxEnd) // Update maxEnd on the way down.
      val cmp: Int = z.compareTo(x)
      if (cmp == 0) {
        return false // Value already in tree. Do nothing.
      }
      x = if (cmp == -1) x.left else x.right
    }

    z.parent = y

    if (y.isNil) {
      root = z
      root.blacken()
    } else {
      // Set the parent of n.
      val cmp: Int = z.compareTo(y)
      if (cmp == -1) {
        y.left = z
      } else {
        assert(cmp == 1)
        y.right = z
      }
      z.left = nil
      z.right = nil
      z.redden()
      z.insertFixup()
    }

    size++
    return true
  }

  fun insertAll(entries: Iterable<T>) {
    // we intentionally shuffle the entries to insert them randomly to try to avoid the
    // worst case insert performance
    for (entry in entries.shuffled()) {
      insert(entry)
    }
  }

  /** @see insertAll */
  fun insertAll(entries: Sequence<T>) = insertAll(entries.asIterable())

  // ////////////////////////////
  // Tree -- Deletion methods //
  // ////////////////////////////
  /**
   * Deletes the given value from this IntervalTree.
   *
   *
   * If the value does not exist, this IntervalTree remains unchanged.
   * @param t - the Interval to delete from the tree
   * @return whether or not an Interval was removed from this IntervalTree
   */
  fun delete(t: T): Boolean { // Node#delete does nothing and returns
    return search(t).delete() // false if t.isNil
  }

  /**
   * Deletes the smallest Interval from this IntervalTree.
   *
   *
   * If there is no smallest Interval (that is, if the tree is empty), this
   * IntervalTree remains unchanged.
   * @return whether or not an Interval was removed from this IntervalTree
   */
  fun deleteMin(): Boolean { // Node#delete does nothing and
    return root.minimumNode().delete() // returns false if t.isNil
  }

  /**
   * Deletes the greatest Interval from this IntervalTree.
   *
   *
   * If there is no greatest Interval (that is, if the tree is empty), this
   * IntervalTree remains unchanged.
   * @return whether or not an Interval was removed from this IntervalTree
   */
  fun deleteMax(): Boolean { // Node#delete does nothing and
    return root.maximumNode().delete() // returns false if t.isNil
  }

  /**
   * Deletes all Intervals that overlap the given Interval from this
   * IntervalTree.
   *
   *
   * If there are no overlapping Intervals, this IntervalTree remains
   * unchanged.
   * @param interval - the overlapping Interval
   * @return whether or not an Interval was removed from this IntervalTree
   */
  fun deleteOverlappers(interval: T): Boolean {
    val nodesToDelete = OverlappingNodeIterator(root, interval).asSequence()
      .distinct()
      .toList()
      .reversed()
    return nodesToDelete.map { it.delete() }.any()
  }

  /**
   * A representation of a node in an interval tree.
   */
  private inner class Node : Interval {
    // Most of the "guts" of the interval tree are actually methods called
    // by nodes. For example, IntervalTree#delete(val) searches up the Node
    // containing val; then that Node deletes itself with Node#delete().

    var interval: T? = null
      private set

    var parent: Node

    var left: Node
    var right: Node

    var isBlack = false
      private set

    var maxEnd = 0L

    /**
     * Constructs a Node with no data.
     *
     *
     * This Node has a null interval field, is black, and has all pointers
     * pointing at itself. This is intended to be used as the sentinel
     * node in the tree ("nil" in CLRS).
     */
    constructor() {
      parent = this
      left = this
      right = this
      blacken()
    }

    /**
     * Constructs a Node containing the given Interval.
     * @param data - the Interval to be contained within this Node
     */
    constructor(interval: T) {
      this.interval = interval
      parent = nil
      left = nil
      right = nil
      maxEnd = interval.endExclusive
      redden()
    }

    /**
     * The start of the Interval in this Node
     */
    override val start: Long
      get() = interval!!.start

    /**
     * The end of the Interval in this Node
     */
    override val endExclusive: Long
      get() = interval!!.endExclusive

    // /////////////////////////////////
    // Node -- General query methods //
    // /////////////////////////////////
    /**
     * Searches the subtree rooted at this Node for the given Interval.
     * @param t - the Interval to search for
     * @return the Node with the given Interval, if it exists; otherwise,
     * the sentinel Node
     */
    fun search(t: Interval): Node {
      var n = this
      while (!n.isNil && t.compareTo(n) != 0) {
        n = if (t.compareTo(n) == -1) n.left else n.right
      }
      return n
    }

    /**
     * Searches the subtree rooted at this Node for the given Interval.
     * @param t - the Interval to search for
     * @return the Node with the given Interval, if it exists; otherwise,
     * the sentinel Node
     */
    fun closestSuccessor(t: Interval): Node {
      var n = this
      var closestSuccessor = if (n.compareTo(t) == 1) n else nil
      while (!n.isNil && t.compareTo(n) != 0) {
        val compare = t.compareTo(n)
        if (compare == -1) {
          closestSuccessor = if (closestSuccessor == nil) n else minOf(closestSuccessor, n)
        }

        n = if (compare == -1) n.left else n.right
      }

      return closestSuccessor
    }

    /**
     * Searches the subtree rooted at this Node for its minimum Interval.
     * @return the Node with the minimum Interval, if it exists; otherwise,
     * the sentinel Node
     */
    fun minimumNode(): Node {
      var n: Node = this
      while (!n.left.isNil) {
        n = n.left
      }
      return n
    }

    /**
     * Searches the subtree rooted at this Node for its maximum Interval.
     * @return the Node with the maximum Interval, if it exists; otherwise,
     * the sentinel Node
     */
    fun maximumNode(): Node {
      var n: Node = this
      while (!n.right.isNil) {
        n = n.right
      }
      return n
    }

    /**
     * The successor of this Node.
     * @return the Node following this Node, if it exists; otherwise the
     * sentinel Node
     */
    fun successor(): Node {
      if (!right.isNil) {
        return right.minimumNode()
      }
      var x: Node = this
      var y: Node = parent
      while (!y.isNil && x == y.right) {
        x = y
        y = y.parent
      }
      return y
    }

    /**
     * The predecessor of this Node.
     * @return the Node preceding this Node, if it exists; otherwise the
     * sentinel Node
     */
    fun predecessor(): Node {
      if (!left.isNil) {
        return left.maximumNode()
      }
      var x: Node = this
      var y: Node = parent
      while (!y.isNil && x == y.left) {
        x = y
        y = y.parent
      }
      return y
    }

    // /////////////////////////////////////
    // Node -- Overlapping query methods //
    // /////////////////////////////////////

    /**
     * Returns a Node from this Node's subtree that overlaps the given
     * Interval.
     *
     *
     * The only guarantee of this method is that the returned Node overlaps
     * the Interval t. This method is meant to be a quick helper method to
     * determine if any overlap exists between an Interval and any of an
     * IntervalTree's Intervals. The returned Node will be the first
     * overlapping one found.
     * @param t - the given Interval
     * @return an overlapping Node from this Node's subtree, if one exists;
     * otherwise the sentinel Node
     */
    fun anyOverlappingNode(t: Interval): Node {
      var x: Node = this
      while (!x.isNil && !t.overlaps(x.interval!!)) {
        x = if (!x.left.isNil && x.left.maxEnd > t.start) x.left else x.right
      }
      return x
    }

    /**
     * Returns the minimum Node from this Node's subtree that overlaps the
     * given Interval.
     * @param t - the given Interval
     * @return the minimum Node from this Node's subtree that overlaps the
     * Interval t, if one exists; otherwise, the sentinel Node
     */
    fun minimumOverlappingNode(t: Interval): Node {
      var result: Node = nil
      var n: Node = this
      if (!n.isNil && n.maxEnd > t.start) {
        while (true) {
          if (n.overlaps(t)) {
            // This node overlaps. There may be a lesser overlapper
            // down the left subtree. No need to consider the right
            // as all overlappers there will be greater.
            result = n
            n = n.left
            if (n.isNil || n.maxEnd <= t.start) {
              // Either no left subtree, or nodes can't overlap.
              break
            }
          } else {
            // This node doesn't overlap.
            // Check the left subtree if an overlapper may be there
            val left: Node = n.left
            if (!left.isNil && left.maxEnd > t.start) {
              n = left
            } else {
              // Left subtree cannot contain an overlapper. Check the
              // right sub-tree.
              if (n.start >= t.endExclusive) {
                // Nothing in the right subtree can overlap
                break
              }
              n = n.right
              if (n.isNil || n.maxEnd <= t.start) {
                // No right subtree, or nodes can't overlap.
                break
              }
            }
          }
        }
      }
      return result
    }

    /**
     * An Iterator over all values in this Node's subtree that overlap the
     * given Interval t.
     * @param t - the overlapping Interval
     */
    fun overlappers(t: Interval): Iterator<T> {
      return OverlapperIterator(this, t)
    }

    /**
     * The next Node (relative to this Node) which overlaps the given
     * Interval t
     * @param t - the overlapping Interval
     * @return the next Node that overlaps the Interval t, if one exists;
     * otherwise, the sentinel Node
     */
    fun nextOverlappingNode(t: Interval): Node {
      var x: Node = this
      var rtrn: Node = nil

      // First, check the right subtree for its minimum overlapper.
      if (!right.isNil) {
        rtrn = x.right.minimumOverlappingNode(t)
      }

      // If we didn't find it in the right subtree, walk up the tree and
      // check the parents of left-children as well as their right subtrees.
      while (!x.parent.isNil && rtrn.isNil) {
        if (x.isLeftChild) {
          rtrn = if (x.parent.overlaps(t)) x.parent else x.parent.right.minimumOverlappingNode(t)
        }
        x = x.parent
      }
      return rtrn
    }

    /**
     * The number of Nodes in this Node's subtree that overlap the given
     * Interval t.
     *
     *
     * This number includes this Node if this Node overlaps t. This method
     * iterates over all overlapping Nodes, so if you ultimately need to
     * inspect the Nodes, it will be more efficient to simply create the
     * Iterator yourself.
     * @param t - the overlapping Interval
     * @return the number of overlapping Nodes
     */
    fun numOverlappingNodes(t: Interval): Long {
      var count = 0L
      val iter = OverlappingNodeIterator(this, t)
      while (iter.hasNext()) {
        iter.next()
        count++
      }
      return count
    }

    // ////////////////////////////
    // Node -- Deletion methods //
    // ////////////////////////////

    /**
     * Deletes this Node from its tree.
     *
     *
     * More specifically, removes the data held within this Node from the
     * tree. Depending on the structure of the tree at this Node, this
     * particular Node instance may not be removed; rather, a different
     * Node may be deleted and that Node's contents copied into this one,
     * overwriting the previous contents.
     */
    fun delete(): Boolean {
      if (isNil) { // Can't delete the sentinel node.
        return false
      }

      var y = this
      if (hasTwoChildren()) {
        // If the node to remove has two children,
        y = successor() // copy the successor's data into it and
        copyData(y) // remove the successor. The successor is
        maxEndFixup() // guaranteed to both exist and have at most
      }

      // one child, so we've converted the two-
      // child case to a one- or no-child case.
      val x = if (y.left.isNil) y.right else y.left
      x.parent = y.parent

      if (y.isRoot) {
        root = x
      } else if (y.isLeftChild) {
        y.parent.left = x
        y.maxEndFixup()
      } else {
        y.parent.right = x
        y.maxEndFixup()
      }

      if (y.isBlack) {
        x.deleteFixup()
      }

      size--
      return true
    }

    // //////////////////////////////////////////////
    // Node -- Tree-invariant maintenance methods //
    // //////////////////////////////////////////////

    /**
     * Whether or not this Node is the root of its tree.
     */
    val isRoot: Boolean
      get() = !isNil && parent.isNil

    /**
     * Whether or not this Node is the sentinel node.
     */
    val isNil: Boolean
      get() = this == nil

    /**
     * Whether or not this Node is the left child of its parent.
     */
    val isLeftChild: Boolean
      get() = this == parent.left

    /**
     * Whether or not this Node is the right child of its parent.
     */
    val isRightChild: Boolean
      get() = this == parent.right

    /**
     * Whether or not this Node has no children, i.e., is a leaf.
     */
    fun hasNoChildren(): Boolean {
      return left.isNil && right.isNil
    }

    /**
     * Whether or not this Node has two children, i.e., neither of its
     * children are leaves.
     */
    fun hasTwoChildren(): Boolean {
      return !left.isNil && !right.isNil
    }

    /**
     * Sets this Node's color to black.
     */
    fun blacken() {
      isBlack = true
    }

    /**
     * Sets this Node's color to red.
     */
    fun redden() {
      isBlack = false
    }

    /**
     * Whether or not this Node's color is red.
     */
    val isRed: Boolean
      get() = !isBlack

    /**
     * A pointer to the grandparent of this Node.
     */
    fun grandparent(): Node {
      return parent.parent
    }

    /**
     * Sets the maxEnd value for this Node.
     *
     *
     * The maxEnd value should be the highest of:
     *
     *  * the end value of this node's data
     *  * the maxEnd value of this node's left child, if not null
     *  * the maxEnd value of this node's right child, if not null
     *
     *
     * This method will be correct only if the left and right children have
     * correct maxEnd values.
     */
    fun resetMaxEnd() {
      var value = interval!!.endExclusive

      if (!left.isNil) {
        value = value.coerceAtLeast(left.maxEnd)
      }

      if (!right.isNil) {
        value = value.coerceAtLeast(right.maxEnd)
      }

      maxEnd = value
    }

    /**
     * Sets the maxEnd value for this Node, and all Nodes up to the root of
     * the tree.
     */
    fun maxEndFixup() {
      var n = this
      n.resetMaxEnd()

      while (!n.parent.isNil) {
        n = n.parent
        n.resetMaxEnd()
      }
    }

    /**
     * Performs a left-rotation on this Node.
     * @see - Cormen et al. "Introduction to Algorithms", 2nd ed, pp. 277-279.
     */
    fun leftRotate() {
      val y = right
      right = y.left

      if (!y.left.isNil) {
        y.left.parent = this
      }

      y.parent = parent
      if (parent.isNil) {
        root = y
      } else if (isLeftChild) {
        parent.left = y
      } else {
        parent.right = y
      }

      y.left = this
      parent = y

      resetMaxEnd()
      y.resetMaxEnd()
    }

    /**
     * Performs a right-rotation on this Node.
     * @see - Cormen et al. "Introduction to Algorithms", 2nd ed, pp. 277-279.
     */
    fun rightRotate() {
      val y = left

      left = y.right
      if (!y.right.isNil) {
        y.right.parent = this
      }

      y.parent = parent
      if (parent.isNil) {
        root = y
      } else if (isLeftChild) {
        parent.left = y
      } else {
        parent.right = y
      }

      y.right = this
      parent = y

      resetMaxEnd()
      y.resetMaxEnd()
    }

    /**
     * Copies the data from a Node into this Node.
     * @param o - the other Node containing the data to be copied
     */
    fun copyData(o: Node) {
      interval = o.interval
    }

    override fun toString(): String {
      return if (isNil) {
        "nil"
      } else {
        val color = if (isBlack) "black" else "red"
        """
        start = $start
        end = $endExclusive
        maxEnd = $maxEnd
        color = $color
        """.trimIndent()
      }
    }

    /**
     * Ensures that red-black constraints and interval-tree constraints are
     * maintained after an insertion.
     */
    fun insertFixup() {
      var z: Node = this
      while (z.parent.isRed) {
        if (z.parent.isLeftChild) {
          val y = z.parent.parent.right

          if (y.isRed) {
            z.parent.blacken()
            y.blacken()
            z.grandparent().redden()
            z = z.grandparent()
          } else {
            if (z.isRightChild) {
              z = z.parent
              z.leftRotate()
            }
            z.parent.blacken()
            z.grandparent().redden()
            z.grandparent().rightRotate()
          }
        } else {
          val y = z.grandparent().left

          if (y.isRed) {
            z.parent.blacken()
            y.blacken()
            z.grandparent().redden()
            z = z.grandparent()
          } else {
            if (z.isLeftChild) {
              z = z.parent
              z.rightRotate()
            }

            z.parent.blacken()
            z.grandparent().redden()
            z.grandparent().leftRotate()
          }
        }
      }

      root.blacken()
    }

    /**
     * Ensures that red-black constraints and interval-tree constraints are
     * maintained after deletion.
     */
    fun deleteFixup() {
      var x = this
      while (!x.isRoot && x.isBlack) {
        if (x.isLeftChild) {
          var w = x.parent.right
          if (w.isRed) {
            w.blacken()
            x.parent.redden()
            x.parent.leftRotate()
            w = x.parent.right
          }

          if (w.left.isBlack && w.right.isBlack) {
            w.redden()
            x = x.parent
          } else {
            if (w.right.isBlack) {
              w.left.blacken()
              w.redden()
              w.rightRotate()
              w = x.parent.right
            }

            w.isBlack = x.parent.isBlack
            x.parent.blacken()
            w.right.blacken()
            x.parent.leftRotate()
            x = root
          }
        } else {
          var w = x.parent.left
          if (w.isRed) {
            w.blacken()
            x.parent.redden()
            x.parent.rightRotate()
            w = x.parent.left
          }

          if (w.left.isBlack && w.right.isBlack) {
            w.redden()
            x = x.parent
          } else {
            if (w.left.isBlack) {
              w.right.blacken()
              w.redden()
              w.leftRotate()
              w = x.parent.left
            }
            w.isBlack = x.parent.isBlack
            x.parent.blacken()
            w.left.blacken()
            x.parent.rightRotate()
            x = root
          }
        }
      }

      x.blacken()
    }

    // /////////////////////////////
    // Node -- Debugging methods //
    // /////////////////////////////
    /**
     * Whether or not the subtree rooted at this Node is a valid
     * binary-search tree.
     * @param min - a lower-bound Node
     * @param max - an upper-bound Node
     */
    @VisibleForTesting
    internal
    fun isBST(min: Node?, max: Node?): Boolean {
      if (isNil) {
        return true // Leaves are a valid BST, trivially.
      }
      if (min != null && compareTo(min) <= 0) {
        return false // This Node must be greater than min
      }
      return if (max != null && compareTo(max) >= 0) {
        false // and less than max.
      } else {
        left.isBST(min, this) && right.isBST(this, max)
      }

      // Children recursively call method with updated min/max.
    }

    /**
     * Whether or not the subtree rooted at this Node is balanced.
     *
     *
     * Balance determination is done by calculating the black-height.
     * @param black - the expected black-height of this subtree
     */
    @VisibleForTesting
    internal
    fun isBalanced(black: Int): Boolean {
      if (isNil) {
        // Leaves have a black-height of zero, even though they are black.
        return black == 0
      }

      var blackValue = if (isBlack) black - 1 else black
      return left.isBalanced(blackValue) && right.isBalanced(blackValue)
    }

    /**
     * Whether or not the subtree rooted at this Node has a valid
     * red-coloring.
     *
     *
     * A red-black tree has a valid red-coloring if every red node has two
     * black children.
     */
    @VisibleForTesting
    internal fun hasValidRedColoring(): Boolean {
      return if (isNil) {
        true
      } else if (isBlack) {
        left.hasValidRedColoring() &&
          right.hasValidRedColoring()
      } else {
        left.isBlack &&
          right.isBlack &&
          left.hasValidRedColoring() &&
          right.hasValidRedColoring()
      }
    }

    /**
     * Whether or not the subtree rooted at this Node has consistent maxEnd
     * values.
     *
     *
     * The maxEnd value of an interval-tree Node is equal to the maximum of
     * the end-values of all intervals contained in the Node's subtree.
     */
    fun hasConsistentMaxEnds(): Boolean {
      if (isNil) { // 1. sentinel node
        return true
      }
      return if (hasNoChildren()) { // 2. leaf node
        maxEnd == endExclusive
      } else {
        val consistent = maxEnd >= endExclusive
        if (hasTwoChildren()) { // 3. two children
          consistent && maxEnd >= left.maxEnd && maxEnd >= right.maxEnd &&
            left.hasConsistentMaxEnds() &&
            right.hasConsistentMaxEnds()
        } else if (left.isNil) { // 4. one child -- right
          consistent && maxEnd >= right.maxEnd &&
            right.hasConsistentMaxEnds()
        } else {
          consistent && // 5. one child -- left
            maxEnd >= left.maxEnd &&
            left.hasConsistentMaxEnds()
        }
      }
    }
  }

  // /////////////////////
  // Tree -- Iterators //
  // /////////////////////
  /**
   * An Iterator which walks along this IntervalTree's Nodes in ascending order.
   */
  private inner class TreeNodeIterator(root: Node) : Iterator<Node> {
    private var next: Node = root.minimumNode()

    override fun hasNext(): Boolean {
      return !next.isNil
    }

    override fun next(): Node {
      if (!hasNext()) {
        throw NoSuchElementException("Interval tree has no more elements.")
      }

      val rtrn: Node = next
      next = rtrn.successor()
      return rtrn
    }
  }

  /**
   * An Iterator which walks along this IntervalTree's Intervals in ascending
   * order.
   *
   *
   * This class just wraps a TreeNodeIterator and extracts each Node's Interval.
   */
  private inner class TreeIterator(root: Node) : Iterator<T> {
    private val nodeIter: TreeNodeIterator = TreeNodeIterator(root)

    override fun hasNext(): Boolean {
      return nodeIter.hasNext()
    }

    override fun next(): T {
      if (!hasNext()) {
        throw NoSuchElementException("Interval tree has no more elements.")
      }

      return nodeIter.next().interval!!
    }
  }

  /**
   * An Iterator which walks along this IntervalTree's Nodes that overlap
   * a given Interval in ascending order.
   */
  private inner class OverlappingNodeIterator(root: Node, private val interval: Interval) : Iterator<Node> {
    private var next: Node = root.minimumOverlappingNode(interval)

    override fun hasNext(): Boolean {
      return !next.isNil
    }

    override fun next(): Node {
      if (!hasNext()) {
        throw NoSuchElementException("Interval tree has no more overlapping elements.")
      }

      val result = next
      next = result.nextOverlappingNode(interval)
      return result
    }
  }

  /**
   * An Iterator which walks along this IntervalTree's Intervals that overlap
   * a given Interval in ascending order.
   *
   *
   * This class just wraps an OverlappingNodeIterator and extracts each Node's
   * Interval.
   */
  private inner class OverlapperIterator(root: Node, t: Interval) : Iterator<T> {
    private val nodeIter = OverlappingNodeIterator(root, t)

    override fun hasNext(): Boolean {
      return nodeIter.hasNext()
    }

    override fun next(): T {
      if (!hasNext()) {
        throw NoSuchElementException("Interval tree has no more overlapping elements.")
      }

      return nodeIter.next().interval!!
    }
  }

  // /////////////////////////////
  // Tree -- Debugging methods //
  // /////////////////////////////
  /**
   * Whether or not this IntervalTree is a valid binary-search tree.
   *
   *
   * This method will return false if any Node is less than its left child
   * or greater than its right child.
   *
   *
   * This method is used for debugging only, and its access is changed in
   * testing.
   */
  @VisibleForTesting
  internal fun isBST(): Boolean = root.isBST(null, null)

  /**
   * Whether or not this IntervalTree is balanced.
   *
   *
   * This method will return false if all of the branches (from root to leaf)
   * do not contain the same number of black nodes. (Specifically, the
   * black-number of each branch is compared against the black-number of the
   * left-most branch.)
   *
   *
   * This method is used for debugging only, and its access is changed in
   * testing.
   */
  @VisibleForTesting
  internal fun isBalanced(): Boolean {
    var black = 0
    var x: Node = root
    while (!x.isNil) {
      if (x.isBlack) {
        black++
      }
      x = x.left
    }
    return root.isBalanced(black)
  }

  /**
   * Whether or not this IntervalTree has a valid red coloring.
   *
   *
   * This method will return false if all of the branches (from root to leaf)
   * do not contain the same number of black nodes. (Specifically, the
   * black-number of each branch is compared against the black-number of the
   * left-most branch.)
   *
   *
   * This method is used for debugging only, and its access is changed in
   * testing.
   */
  @VisibleForTesting
  internal fun hasValidRedColoring(): Boolean = root.hasValidRedColoring()

  /**
   * Whether or not this IntervalTree has consistent maxEnd values.
   *
   *
   * This method will only return true if each Node has a maxEnd value equal
   * to the highest interval end value of all the intervals in its subtree.
   *
   *
   * This method is used for debugging only, and its access is changed in
   * testing.
   */
  @VisibleForTesting
  internal fun hasConsistentMaxEnds(): Boolean = root.hasConsistentMaxEnds()
}
