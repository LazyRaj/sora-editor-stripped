/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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
/* Modified by Raj Dave (aka LazyRaj) */
package io.github.rosemoe.oniguruma

/**
 * Oniguruma native function bridge.
 * Use with carefulness. Dirty pointer may cause a direct crash of your app.
 *
 * @author Rosemoe
 */
object OnigNative {

    init {
        System.loadLibrary("oniguruma-binding")
    }

    /**
     * Create a new OnigRegex
     *
     * @param pattern Pattern string
     * @param ignoreCase Ignore case when matching
     * @return The pointer of newly created regex, or 0L if it fails
     */
    fun newRegex(pattern: String, ignoreCase: Boolean = false): Long {
        return newRegex(pattern.toByteArray(Charsets.UTF_8), ignoreCase)
    }

    /**
     * Create a new OnigRegex
     *
     * @param pattern UTF-8 Bytes of the pattern string
     * @param ignoreCase Ignore case when matching
     * @return The pointer of newly created regex, or 0L if it fails
     */
    fun newRegex(pattern: ByteArray, ignoreCase: Boolean = false): Long {
        return nCreateRegex(pattern, ignoreCase)
    }

    /**
     * Release a OnigRegex previously created by [newRegex]
     *
     * @param nativePtr Native pointer. Passing null pointer will have no effect.
     */
    @JvmStatic
    external fun releaseRegex(nativePtr: Long)

    /**
     * Search using the given OnigRegex.
     *
     * @param nativePointer OnigRegex pointer from [newRegex]
     * @param cacheKey The cache key for the source string
     * @param str String bytes to be searched in
     * @param start Start position in the string (inclusive)
     * @param end End position in the string (exclusive)
     * @return Ranges if the match is successful. Each range is represented as two integers start and end.
     * Null if the match failed.
     */
    fun regexSearch(nativePointer: Long, cacheKey: Long, str: ByteArray, start: Int, end: Int): IntArray? {
        if (start > end || start < 0 || end > str.size) {
            throw IndexOutOfBoundsException("start:$start end:$end str.length:${str.size}")
        }
        return nRegexSearch(nativePointer, cacheKey, str, start, end)
    }

    /**
     * Search using the given OnigRegex list
     *
     * @param nativePointers OnigRegex pointers from [newRegex]
     * @param cacheKey The cache key for the source string
     * @param str String bytes to be searched in
     * @param start Start position in the string (inclusive)
     * @param end End position in the string (exclusive)
     * @return Ranges if the match is successful. Each range is represented as two integers start and end.
     * Null if the match failed.
     */
    fun regexSearchBatch(nativePointers: LongArray, cacheKey: Long, str: ByteArray, start: Int, end: Int): IntArray? {
        if (start > end || start < 0 || end > str.size) {
            throw IndexOutOfBoundsException("start:$start end:$end str.length:${str.size}")
        }
        return nRegexSearchBatch(nativePointers, cacheKey, str, start, end)
    }

    @JvmStatic
    private external fun nCreateRegex(pattern: ByteArray, ignoreCase: Boolean): Long

    @JvmStatic
    private external fun nRegexSearch(nativePtr: Long, cacheKey: Long, str: ByteArray, start: Int, end: Int): IntArray?

    @JvmStatic
    private external fun nRegexSearchBatch(nativePtrs: LongArray, cacheKey: Long, str: ByteArray, start: Int, end: Int): IntArray?
}
