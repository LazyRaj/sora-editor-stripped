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
package io.github.rosemoe.sora.langs.textmate.registry

import io.github.rosemoe.sora.langs.textmate.registry.provider.FileResolver
import java.io.InputStream
import java.util.concurrent.CopyOnWriteArrayList

class FileProviderRegistry private constructor() {

    private val allFileResolvers = CopyOnWriteArrayList<FileResolver>().apply {
        add(FileResolver.DEFAULT)
    }

    @Synchronized
    fun addFileProvider(fileResolver: FileResolver) {
        if (fileResolver != FileResolver.DEFAULT && !allFileResolvers.contains(fileResolver)) {
            allFileResolvers.add(fileResolver)
        }
    }

    @Synchronized
    fun removeFileProvider(fileResolver: FileResolver) {
        if (fileResolver != FileResolver.DEFAULT) {
            allFileResolvers.remove(fileResolver)
        }
    }

    fun tryGetInputStream(path: String): InputStream? {
        for (provider in allFileResolvers) {
            val stream = provider.resolveStreamByPath(path)
            if (stream != null) {
                return stream
            }
        }
        return null
    }

    fun dispose() {
        for (provider in allFileResolvers) {
            provider.dispose()
        }
        allFileResolvers.clear()
    }

    companion object {
        @Volatile
        private var instance: FileProviderRegistry? = null

        @Synchronized
        fun getInstance(): FileProviderRegistry {
            return instance ?: FileProviderRegistry().also { instance = it }
        }
    }
}
