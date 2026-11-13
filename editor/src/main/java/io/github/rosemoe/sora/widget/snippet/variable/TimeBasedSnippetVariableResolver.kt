/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
/*
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.widget.snippet.variable

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Resolver for time-related variables
 *
 * @author Rosemoe
 */
open class TimeBasedSnippetVariableResolver : ISnippetVariableResolver {

    companion object {
        private fun getDisplayName(field: Int, shortType: Boolean): String {
            val c = Calendar.getInstance()
            var result = c.getDisplayName(field, if (shortType) Calendar.SHORT else Calendar.LONG, Locale.getDefault())
            if (result == null && shortType) {
                result = c.getDisplayName(field, Calendar.LONG, Locale.getDefault())
            }
            if (result == null) {
                result = c.getDisplayName(field, if (shortType) Calendar.SHORT else Calendar.LONG, Locale.US)
            }
            if (result == null) {
                // The very fallback
                result = c.get(field).toString()
            }
            return result
        }
    }

    override fun getResolvableNames(): Array<String> {
        return arrayOf(
            "CURRENT_YEAR", "CURRENT_YEAR_SHORT", "CURRENT_MONTH", "CURRENT_DATE",
            "CURRENT_HOUR", "CURRENT_MINUTE", "CURRENT_SECOND", "CURRENT_DAY_NAME",
            "CURRENT_DAY_NAME_SHORT", "CURRENT_MONTH_NAME", "CURRENT_MONTH_NAME_SHORT",
            "CURRENT_SECONDS_UNIX"
        )
    }

    override fun resolve(name: String): String {
        return when (name) {
            "CURRENT_YEAR" -> Calendar.getInstance().get(Calendar.YEAR).toString()
            "CURRENT_YEAR_SHORT" -> (Calendar.getInstance().get(Calendar.YEAR) % 100).toString().padStart(2, '0')
            "CURRENT_MONTH" -> Calendar.getInstance().get(Calendar.MONTH).toString().padStart(2, '0')
            "CURRENT_DATE" -> SimpleDateFormat.getDateInstance().format(Date())
            "CURRENT_HOUR" -> Calendar.getInstance().get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
            "CURRENT_MINUTE" -> Calendar.getInstance().get(Calendar.MINUTE).toString().padStart(2, '0')
            "CURRENT_SECOND" -> Calendar.getInstance().get(Calendar.SECOND).toString().padStart(2, '0')
            "CURRENT_DAY_NAME" -> getDisplayName(Calendar.DAY_OF_WEEK, false)
            "CURRENT_DAY_NAME_SHORT" -> getDisplayName(Calendar.DAY_OF_WEEK, true)
            "CURRENT_MONTH_NAME" -> getDisplayName(Calendar.MONTH, false)
            "CURRENT_MONTH_NAME_SHORT" -> getDisplayName(Calendar.MONTH, true)
            "CURRENT_SECONDS_UNIX" -> Math.round(System.currentTimeMillis() / 1000.0).toString()
            else -> throw IllegalArgumentException("Unsupported variable name:$name")
        }
    }
}
