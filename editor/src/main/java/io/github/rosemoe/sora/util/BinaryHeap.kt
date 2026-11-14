/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *    Modified by Raj Dave (aka LazyRaj)
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.util

import android.util.SparseIntArray
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * A general implementation of big root heap
 *
 * @author Rosemoe
 */
class BinaryHeap {

    companion object {
        private fun id(value: Long): Int = IntPair.getFirst(value)
        private fun data(value: Long): Int = IntPair.getSecond(value)
    }

    /**
     * Lock for multi-thread reusing
     */
    val lock: Lock = ReentrantLock()

    /**
     * Map from id to its position in heap array
     */
    private val idToPosition = SparseIntArray()

    /**
     * Id allocator
     */
    private var idAllocator = 1

    /**
     * Current node count in heap
     */
    var nodeCount: Int = 0
        private set

    /**
     * Node array for heap.
     * first:  id
     * second: data
     */
    private var nodes = LongArray(129)

    /**
     * Clear all the nodes in the heap
     */
    fun clear() {
        nodeCount = 0
        idToPosition.clear()
        idAllocator = -1
    }

    /**
     * Ensure there is enough space
     *
     * @param capacity desired space size
     */
    fun ensureCapacity(capacity: Int) {
        val targetCapacity = capacity + 1
        if (nodes.size < targetCapacity) {
            val origin = nodes
            nodes = if ((nodes.size shl 1) >= targetCapacity) {
                LongArray(nodes.size shl 1)
            } else {
                LongArray(targetCapacity)
            }
            System.arraycopy(origin, 0, nodes, 0, nodeCount + 1)
        }
    }

    /**
     * Get the max value in this heap or zero if no node is in heap
     *
     * @return Max value
     */
    val top: Int get() = top()

    fun top(): Int {
        if (nodeCount == 0) {
            return 0
        }
        return data(nodes[1])
    }

    /**
     * Internal implementation to move down nodes
     *
     * @param position target node's position in heap
     */
    private fun heapifyDown(position: Int) {
        var currentPos = position
        var child = currentPos * 2
        while (child <= nodeCount) {
            val parentNode = nodes[currentPos]
            if (child + 1 <= nodeCount && data(nodes[child + 1]) > data(nodes[child])) {
                child++
            }
            val childNode = nodes[child]
            if (data(parentNode) < data(childNode)) {
                idToPosition.put(id(childNode), currentPos)
                idToPosition.put(id(parentNode), child)
                nodes[child] = parentNode
                nodes[currentPos] = childNode
                currentPos = child
            } else {
                break
            }
            child = currentPos * 2
        }
    }

    /**
     * Internal implementation to move up nodes
     *
     * @param position target node's position in heap
     */
    private fun heapifyUp(position: Int) {
        var currentPos = position
        var parent = currentPos / 2
        while (parent >= 1) {
            val childNode = nodes[currentPos]
            val parentNode = nodes[parent]
            if (data(childNode) > data(parentNode)) {
                idToPosition.put(id(childNode), parent)
                idToPosition.put(id(parentNode), currentPos)
                nodes[currentPos] = parentNode
                nodes[parent] = childNode
                currentPos = parent
            } else {
                break
            }
            parent = currentPos / 2
        }
    }

    /**
     * Add a new node to the heap
     *
     * @return ID of node
     * @throws IllegalStateException when there is no new id available
     */
    fun push(value: Int): Int {
        ensureCapacity(nodeCount + 1)
        if (idAllocator == Int.MAX_VALUE) {
            throw IllegalStateException("unable to allocate more id")
        }
        val id = idAllocator++
        nodeCount++
        nodes[nodeCount] = IntPair.pack(id, value)
        idToPosition.put(id, nodeCount)
        heapifyUp(nodeCount)
        return id
    }

    /**
     * Update the value of node with given id to newValue
     *
     * @param id       ID returned by push()
     * @param newValue new value for this node
     * @throws IllegalArgumentException when the id is invalid
     */
    fun update(id: Int, newValue: Int) {
        val position = idToPosition.get(id, 0)
        if (position == 0) {
            throw IllegalArgumentException("trying to update with an invalid id")
        }
        val origin = data(nodes[position])
        nodes[position] = IntPair.pack(id(nodes[position]), newValue)
        if (origin < newValue) {
            heapifyUp(position)
        } else if (origin > newValue) {
            heapifyDown(position)
        }
    }

    /**
     * Remove node with given id
     *
     * @param id ID returned by push()
     * @throws IllegalArgumentException when the id is invalid
     */
    fun remove(id: Int) {
        val position = idToPosition.get(id, 0)
        if (position == 0) {
            throw IllegalArgumentException("trying to remove with an invalid id")
        }
        idToPosition.delete(id)
        // Replace removed node with last node
        nodes[position] = nodes[nodeCount]
        // Release node
        nodes[nodeCount--] = 0
        // Do not update heap if it is just the last node
        if (position == nodeCount + 1) {
            return
        }
        idToPosition.put(id(nodes[position]), position)
        heapifyUp(position)
        heapifyDown(position)
    }
}
