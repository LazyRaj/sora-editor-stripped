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

import kotlin.math.abs
import kotlin.math.max

/**
 * TrieTree to query values quickly
 *
 * @author Rose
 */
class TrieTree<T> {

    val root: Node<T> = Node()
    private var maxLen: Int = 0

    fun put(v: String, token: T) {
        maxLen = max(v.length, maxLen)
        addInternal(root, v, 0, v.length, token)
    }

    fun put(v: CharSequence, off: Int, len: Int, token: T) {
        maxLen = max(maxLen, len)
        addInternal(root, v, off, len, token)
    }

    fun get(s: CharSequence, offset: Int, len: Int): T? {
        if (len > maxLen) {
            return null
        }
        return getInternal(root, s, offset, len)
    }

    private fun getInternal(node: Node<T>, s: CharSequence, offset: Int, len: Int): T? {
        if (len == 0) {
            return node.token
        }
        val point = s[offset]
        val sub = node.map[point] ?: return null
        return getInternal(sub, s, offset + 1, len - 1)
    }

    private fun addInternal(node: Node<T>, v: CharSequence, i: Int, len: Int, token: T) {
        val point = v[i]
        var sub = node.map[point]
        if (sub == null) {
            sub = Node()
            node.map.put(point, sub)
        }
        if (len == 1) {
            sub.token = token
        } else {
            addInternal(sub, v, i + 1, len - 1, token)
        }
    }

    class Node<T> {
        val map: HashCharMap<Node<T>> = HashCharMap()
        var token: T? = null
    }

    /**
     * Hashmap with fixed length
     *
     * @author Rosemoe
     */
    class HashCharMap<V> {
        companion object {
            private const val CAPACITY = 64

            private fun position(first: Int): Int {
                return abs(first xor ((first shl 6) * (if ((first and 1) != 0) 3 else 1))) % CAPACITY
            }
        }

        private val columns: Array<LinkedPair<V>?> = arrayOfNulls(CAPACITY)
        private val ends: Array<LinkedPair<V>?> = arrayOfNulls(CAPACITY)

        operator fun get(first: Char): V? {
            val pos = position(first.code)
            var pair = columns[pos]
            while (pair != null) {
                if (pair.first == first) {
                    return pair.second
                }
                pair = pair.next
            }
            return null
        }

        private fun get(first: Char, position: Int): LinkedPair<V>? {
            var pair = columns[position]
            while (pair != null) {
                if (pair.first == first) {
                    return pair
                }
                pair = pair.next
            }
            return null
        }

        fun put(first: Char, second: V) {
            val pos = position(first.code)
            val endNode = ends[pos]
            if (endNode == null) {
                val pair = LinkedPair<V>().apply {
                    this.first = first
                    this.second = second
                }
                columns[pos] = pair
                ends[pos] = pair
                return
            }
            var p = get(first, pos)
            if (p == null) {
                p = LinkedPair()
                endNode.next = p
                ends[pos] = p
            }
            p.first = first
            p.second = second
        }
    }

    class LinkedPair<V> {
        var next: LinkedPair<V>? = null
        var first: Char = '\u0000'
        var second: V? = null
    }
}
