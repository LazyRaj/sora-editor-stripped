/*******************************************************************************
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
 *
 *     Modified by Raj Dave (aka LazyRaj)
 ******************************************************************************/

package io.github.rosemoe.sora.lsp.editor

import androidx.annotation.WorkerThread
import io.github.rosemoe.sora.lsp.client.languageserver.ServerStatus
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.AggregatedRequestManager
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper
import org.eclipse.lsp4j.ServerCapabilities

internal class LspEditorDelegate(private val editor: LspEditor) {

    private data class SessionInfo(
        val definition: LanguageServerDefinition,
        val wrapper: LanguageServerWrapper
    )

    private val sessionInfos = mutableListOf<SessionInfo>()

    val aggregatedRequestManager = AggregatedRequestManager(emptySet())

    private fun refreshSessions() {
        val definitions =
            editor.project.getServerDefinitions(
                ext = editor.fileExt,
                languageId = editor.languageId,
            )

        sessionInfos.removeAll { session ->
            definitions.none { it.name == session.definition.name && it.ext == session.definition.ext }
        }

        definitions.forEach { definition ->
            val exists = sessionInfos.any {
                it.definition.name == definition.name && it.definition.ext == definition.ext
            }
            if (!exists) {
                val wrapper =
                    editor.project.getOrCreateLanguageServerWrapper(definition.ext, definition.name)
                sessionInfos.add(SessionInfo(definition, wrapper))
            }
        }
    }

    fun onWrapperDisconnected(wrapper: LanguageServerWrapper) {
        sessionInfos.removeAll { it.wrapper === wrapper }
        val remaining = sessionInfos
            .filter { it.wrapper.status == ServerStatus.INITIALIZED }
            .map { it.wrapper }
            .toSet()
        aggregatedRequestManager.updateSessions(remaining)
    }

    @WorkerThread
    fun connectAll(): ServerCapabilities? {
        refreshSessions()
        var lastCapabilities: ServerCapabilities? = null

        for (info in sessionInfos) {
            info.wrapper.start()
            val capabilities = info.wrapper.getServerCapabilities()

            info.wrapper.connect(editor)
            lastCapabilities = capabilities ?: lastCapabilities
        }

        val initializedWrappers = sessionInfos
            .filter { it.wrapper.status == ServerStatus.INITIALIZED }
            .map { it.wrapper }
            .toSet()

        aggregatedRequestManager.updateSessions(initializedWrappers)

        return aggregatedRequestManager.capabilities ?: lastCapabilities
    }

    fun disconnectAll() {
        sessionInfos.forEach { it.wrapper.disconnect(editor) }
        sessionInfos.clear()
    }

    val primaryWrapper: LanguageServerWrapper?
        get() = sessionInfos.firstOrNull()?.wrapper
}
