/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

object PreserveCaseReplace {

    @JvmStatic
    fun getReplacementSimple(oldStr: String, newStr: String): String {
        if (oldStr.isEmpty() || newStr.isEmpty()) return newStr
        // Analyze the case of old string
        var isUpperCase = true
        var isLowerCase = true
        var isCapitalized = true
        for (i in 0 until oldStr.length) {
            val ch = oldStr[i]
            if (!Character.isLetter(ch)) continue
            val upper = Character.isUpperCase(ch)
            val lower = Character.isLowerCase(ch)
            if (isUpperCase && isLowerCase) {
                isCapitalized = upper
            } else {
                isCapitalized = isCapitalized and lower
            }
            isUpperCase = isUpperCase and upper
            isLowerCase = isLowerCase and lower
        }
        // No letter found
        if (isUpperCase && isLowerCase) return newStr
        // Upper / Lower / Capitalized
        if (isUpperCase) return newStr.uppercase()
        if (isLowerCase) return newStr.lowercase()
        if (isCapitalized) {
            return newStr.substring(0, 1).uppercase() + newStr.substring(1).lowercase()
        }
        // Mixed case
        val sb = StringBuilder()
        for (i in 0 until newStr.length) {
            val ch = newStr[i]
            if (!Character.isLetter(ch) || i >= oldStr.length) {
                sb.append(ch)
                continue
            }
            val oldCh = oldStr[i]
            if (Character.isLetter(oldCh)) {
                sb.append(if (Character.isUpperCase(oldCh)) ch.uppercaseChar() else ch.lowercaseChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
