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

package io.github.rosemoe.sora.text

/**
 * Reference of a [CharSequence] object, which allows
 * to access the original sequence in read-only mode, and attach a
 * [Validator] to validate access to check whether to reject the
 * read access.
 *
 * @author Rosemoe
 */
open class TextReference(
    reference: CharSequence,
    val start: Int,
    val end: Int
) : CharSequence {

    private val _reference: CharSequence = reference

    open val reference: CharSequence
        get() = _reference

    private var validator: Validator? = null

    constructor(ref: CharSequence) : this(ref, 0, ref.length)

    init {
        if (start > end) {
            throw IllegalArgumentException("start > end")
        }
        if (start < 0) {
            throw StringIndexOutOfBoundsException(start)
        }
        if (end > _reference.length) {
            throw StringIndexOutOfBoundsException(end)
        }
    }

    override val length: Int
        get() {
            validateAccess()
            return end - start
        }

    override fun get(index: Int): Char {
        if (index < 0 || index >= length) {
            throw StringIndexOutOfBoundsException(index)
        }
        validateAccess()
        return reference[start + index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (startIndex < 0 || startIndex >= length) {
            throw StringIndexOutOfBoundsException(startIndex)
        }
        if (endIndex < 0 || endIndex > length) {
            throw StringIndexOutOfBoundsException(endIndex)
        }
        validateAccess()
        return TextReference(
            reference,
            start + startIndex,
            start + endIndex
        ).setValidator(validator)
    }

    override fun toString(): String {
        return reference.subSequence(start, end).toString()
    }

    open fun setValidator(validator: Validator?): TextReference {
        this.validator = validator
        return this
    }

    open fun validateAccess() {
        validator?.validate()
    }

    fun interface Validator {
        fun validate()
    }

    open class ValidateFailedException : RuntimeException {
        constructor() : super()
        constructor(message: String) : super(message)
    }
}
