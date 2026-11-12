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

import java.util.concurrent.atomic.AtomicInteger

class SegmentList<T> @JvmOverloads constructor(
    val segmentCapacity: Int = DEFAULT_SEGMENT_CAPACITY
) : AbstractMutableList<T>() {

    companion object {
        const val DEFAULT_SEGMENT_CAPACITY = 8192
    }

    private val segments: MutableList<Segment<T>>

    private var length: Int = 0

    init {
        if (segmentCapacity < 4) {
            throw IllegalArgumentException("block size should be at least 4")
        }
        segments = ArrayList()
    }

    private fun checkInsertIndex(index: Int) {
        if (index < 0 || index > length) {
            throw IndexOutOfBoundsException("index $index out of bounds. length = $length")
        }
    }

    private fun checkAccessIndex(index: Int) {
        if (index < 0 || index >= length) {
            throw IndexOutOfBoundsException("index $index out of bounds. length = $length")
        }
    }

    private class FindResult<T> {
        var segment: Segment<T>? = null
        var offset: Int = 0
        var blockIndex: Int = 0
    }

    private val result = FindResult<T>()

    private fun makeResult(segment: Segment<T>, offset: Int, segIndex: Int): FindResult<T> {
        result.blockIndex = segIndex
        result.segment = segment
        result.offset = offset
        return result
    }

    private fun getSegment(index: Int): FindResult<T> {
        if (segments.isEmpty()) {
            segments.add(Segment(segmentCapacity))
        }
        var offset = 0
        var backBlock = segments[segments.size - 1]
        var backOffset = size - backBlock.size
        var i = 0
        var j = segments.size - 1
        while (i <= j) {
            var block = segments[i]
            if ((index >= offset && index < offset + block.size) || i + 1 == segments.size) {
                return makeResult(block, offset, i)
            }
            offset += block.size

            block = backBlock
            if ((index >= backOffset && index < backOffset + block.size) || (j == segments.size - 1 && index == length)) {
                return makeResult(block, backOffset, j)
            }
            if (j > 0) {
                backBlock = segments[j - 1]
                backOffset -= backBlock.size
            }
            i++
            j--
        }
        throw IllegalStateException("unreachable")
    }

    private fun getSegmentMut(index: Int): FindResult<T> {
        val res = getSegment(index)
        res.segment = ensureMutable(res.blockIndex)
        return res
    }

    private fun ensureMutable(segIdx: Int): Segment<T> {
        val block = segments[segIdx]
        val n = block.toMutable()
        if (block !== n) {
            segments[segIdx] = n
            block.release()
            return n
        }
        return block
    }

    override fun set(index: Int, element: T): T {
        checkAccessIndex(index)
        val res = getSegmentMut(index)
        return res.segment!!.set(index - res.offset, element)
    }

    override fun add(index: Int, element: T) {
        checkInsertIndex(index)
        val res = getSegmentMut(index)
        val seg = res.segment!!
        seg.add(index - res.offset, element)
        length++
        adjustElements(res.blockIndex, seg)
        if (seg.size >= segmentCapacity) {
            val divPoint = segmentCapacity / 2
            val newSeg = Segment<T>(segmentCapacity)
            val sub = seg.subList(divPoint, seg.size)
            newSeg.addAll(sub)
            sub.clear()
            segments.add(res.blockIndex + 1, newSeg)
        }
    }

    override fun removeAt(index: Int): T {
        checkAccessIndex(index)
        val res = getSegmentMut(index)
        val removed = res.segment!!.removeAt(index - res.offset)
        length--
        mergeSegment(index - 1, index)
        mergeSegment(index, index + 1)
        return removed
    }

    override fun get(index: Int): T {
        checkAccessIndex(index)
        val res = getSegment(index)
        return res.segment!![index - res.offset]
    }

    public override fun removeRange(fromIndex: Int, toIndex: Int) {
        if (fromIndex > toIndex) throw IndexOutOfBoundsException("start > end")
        if (fromIndex < 0 || toIndex > length) {
            throw IndexOutOfBoundsException("start = $fromIndex, end = $toIndex, length = $size")
        }
        if (fromIndex == toIndex) return
        val res = getSegment(fromIndex)
        var offset = res.offset
        var index = res.blockIndex
        var seg = res.segment!!

        while (toIndex - offset > 0 && index < segments.size) {
            val segLength = seg.size
            if (fromIndex <= offset && toIndex >= offset + segLength) {
                // Remove the segment
                segments.removeAt(index)
                seg.release()
            } else {
                ensureMutable(index)
                seg = segments[index]
                val sub = seg.subList(
                    maxOf(fromIndex - offset, 0),
                    minOf(toIndex - offset, segLength)
                )
                sub.clear()
                index++
            }
            offset += segLength
            if (index < segments.size) {
                seg = segments[index]
            }
        }
        mergeSegment(index - 1, index)
        length -= toIndex - fromIndex
    }

    override val size: Int
        get() = length

    override fun clear() {
        for (seg in segments) {
            seg.release()
        }
        segments.clear()
        length = 0
    }

    fun shallowCopy(): SegmentList<T> {
        val list = SegmentList<T>(segmentCapacity)
        list.segments.clear()
        for (seg in segments) {
            seg.retain()
        }
        list.segments.addAll(segments)
        list.length = length
        return list
    }

    private fun mergeSegment(seg1Param: Int, seg2Param: Int) {
        var seg1 = seg1Param
        var seg2 = seg2Param
        if (seg1 > seg2) {
            val tmp = seg1
            seg1 = seg2
            seg2 = tmp
        }
        if (seg1 == seg2 || seg1 < 0 || seg2 >= segments.size) return
        var pre = segments[seg1]
        val aft = segments[seg2]
        if (pre.size + aft.size <= segmentCapacity * 3 / 4) {
            ensureMutable(seg1)
            pre = segments[seg1]
            pre.addAll(aft)
            segments.removeAt(seg2)
            aft.release()
        }
    }

    private fun adjustElements(segIdx: Int, mutCur: Segment<T>) {
        if (segIdx > 0) {
            val pre = segments[segIdx - 1]
            if (pre.isMutable() && pre.size <= segmentCapacity * 4 / 5 && mutCur.size > segmentCapacity * 4 / 5) {
                val sub = mutCur.subList(0, segmentCapacity * 4 / 5 - pre.size)
                pre.addAll(sub)
                sub.clear()
            }
        }
    }

    fun forEachCompat(consumer: ConsumerCompat<T>) {
        for (i in segments.indices) {
            val seg = segments[i]
            for (i1 in 0 until seg.size) {
                consumer.accept(seg[i1])
            }
        }
    }

    fun interface ConsumerCompat<T> {
        fun accept(obj: T)
    }

    private class Segment<T> @JvmOverloads constructor(
        initialCapacity: Int = 10
    ) : ArrayList<T>(initialCapacity), ShareableData<Segment<T>> {

        private val refCount = AtomicInteger(1)

        override fun retain() {
            refCount.incrementAndGet()
        }

        override fun release() {
            if (refCount.decrementAndGet() < 0) {
                throw IllegalStateException("illegal release invocation")
            }
        }

        override fun isMutable(): Boolean {
            return refCount.get() == 1
        }

        override fun toMutable(): Segment<T> {
            return if (isMutable()) this else copy()
        }

        fun copy(): Segment<T> {
            val res = Segment<T>(size)
            res.addAll(this)
            return res
        }
    }
}
