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
/**
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.text

import android.os.Parcel
import android.os.Parcelable
import io.github.rosemoe.sora.annotations.UnsupportedUserUsage
import kotlin.math.abs

/**
 * Helper class for Content to take down modification
 * As well as provide Undo/Redo actions
 *
 * @author Rosemoe
 */
class UndoManager : ContentListener, Parcelable {

    companion object {
        private var sMergeTimeLimit = 8000L

        /**
         * The max time span limit for merging actions in milliseconds
         */
        @JvmStatic
        var mergeTimeLimit: Long
            get() = sMergeTimeLimit
            set(value) {
                sMergeTimeLimit = value
            }

        @JvmField
        val CREATOR: Parcelable.Creator<UndoManager> = object : Parcelable.Creator<UndoManager> {
            override fun createFromParcel(parcel: Parcel): UndoManager {
                val o = UndoManager()
                o.maxUndoStackSize = parcel.readInt()
                o.stackPointer = parcel.readInt()
                o.isUndoEnabled = parcel.readInt() > 0
                var count = parcel.readInt()
                while (count > 0) {
                    val action: ContentAction? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        parcel.readParcelable(UndoManager::class.java.classLoader, ContentAction::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        parcel.readParcelable(UndoManager::class.java.classLoader)
                    }
                    if (action != null) {
                        o.actionStack.add(action)
                    }
                    count--
                }
                return o
            }

            override fun newArray(flags: Int): Array<UndoManager?> {
                return arrayOfNulls(flags)
            }
        }
    }

    private val actionStack: MutableList<ContentAction> = ArrayList()
    private var _undoEnabled: Boolean = false
    private var _maxStackSize: Int = 0
    private var insertAction: InsertAction? = null
    private var deleteAction: DeleteAction? = null
    private var targetContent: Content? = null
    private var replaceMark: Boolean = false
    private var stackPointer: Int = 0
    private var ignoreModification: Boolean = false
    private var forceNewMultiAction: Boolean = false
    private var memorizedCursorRange: TextRange? = null

    /**
     * Create an UndoManager
     */
    constructor() {
        replaceMark = false
        insertAction = null
        deleteAction = null
        stackPointer = 0
        ignoreModification = false
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(_maxStackSize)
        parcel.writeInt(stackPointer)
        parcel.writeInt(if (_undoEnabled) 1 else 0)
        parcel.writeInt(actionStack.size)
        for (contentAction in actionStack) {
            parcel.writeParcelable(contentAction, flags)
        }
    }

    /**
     * Check whether we are currently in undo/redo operations
     */
    val isModifyingContent: Boolean
        get() = ignoreModification

    /**
     * Undo on the given Content
     *
     * @param content Undo Target
     */
    fun undo(content: Content): TextRange? {
        if (canUndo() && !isModifyingContent) {
            ignoreModification = true
            val action = actionStack[stackPointer - 1]
            action.undo(content)
            stackPointer--
            ignoreModification = false
            return action.cursor
        }
        return null
    }

    /**
     * Redo on the given Content
     *
     * @param content Redo Target
     */
    fun redo(content: Content) {
        if (canRedo() && !isModifyingContent) {
            ignoreModification = true
            actionStack[stackPointer].redo(content)
            stackPointer++
            ignoreModification = false
        }
    }

    @UnsupportedUserUsage
    fun onExitBatchEdit() {
        forceNewMultiAction = true
        if (actionStack.isNotEmpty() && actionStack.last() is MultiAction) {
            val action = actionStack.last() as MultiAction
            if (action.actions.size == 1) {
                actionStack[actionStack.size - 1] = action.actions[0]
            }
        }
    }

    /**
     * Whether it can undo
     */
    fun canUndo(): Boolean = isUndoEnabled && (stackPointer > 0)

    /**
     * Whether it can redo
     */
    fun canRedo(): Boolean = isUndoEnabled && (stackPointer < actionStack.size)

    /**
     * Whether this UndoManager is enabled
     */
    var isUndoEnabled: Boolean
        get() = _undoEnabled
        set(enabled) {
            _undoEnabled = enabled
            if (!enabled) {
                cleanStack()
            }
        }

    /**
     * Max undo stack size
     */
    var maxUndoStackSize: Int
        get() = _maxStackSize
        set(maxSize) {
            require(maxSize > 0) {
                "max size can not be zero or smaller. Did you want to disable undo module by calling setUndoEnabled()?"
            }
            _maxStackSize = maxSize
            cleanStack()
        }

    /**
     * Clean stack after add or state change
     * This is to limit stack size
     */
    private fun cleanStack() {
        if (!_undoEnabled) {
            actionStack.clear()
            stackPointer = 0
        } else {
            while (stackPointer > 1 && actionStack.size > _maxStackSize) {
                actionStack.removeAt(0)
                stackPointer--
            }
        }
    }

    /**
     * Clean the stack before pushing
     * If we are not at the end(Undo action executed), remove those actions
     */
    private fun cleanBeforePush() {
        while (stackPointer < actionStack.size) {
            actionStack.removeAt(actionStack.size - 1)
        }
    }

    /**
     * Push a new [ContentAction] to stack
     * It will merge actions if possible
     *
     * @param action New [ContentAction]
     */
    private fun pushAction(content: Content, action: ContentAction) {
        if (!isUndoEnabled) {
            return
        }
        cleanBeforePush()
        if (content.isInBatchEdit()) {
            if (actionStack.isEmpty()) {
                val a = MultiAction()
                a.addAction(action)
                a.cursor = action.cursor
                actionStack.add(a)
                stackPointer++
            } else {
                val a = actionStack[actionStack.size - 1]
                if (a is MultiAction && !forceNewMultiAction) {
                    a.addAction(action)
                } else {
                    val ac = MultiAction()
                    ac.addAction(action)
                    ac.cursor = action.cursor
                    actionStack.add(ac)
                    stackPointer++
                }
            }
        } else {
            if (actionStack.isEmpty()) {
                actionStack.add(action)
                stackPointer++
            } else {
                val last = actionStack[actionStack.size - 1]
                if (last.canMerge(action)) {
                    last.merge(action)
                } else {
                    actionStack.add(action)
                    stackPointer++
                }
            }
        }
        forceNewMultiAction = false
        cleanStack()
    }

    fun exitReplaceMode() {
        val dAction = deleteAction
        val tContent = targetContent
        if (replaceMark && dAction != null && tContent != null) {
            pushAction(tContent, dAction)
        }
        replaceMark = false
        targetContent = null
    }

    override fun beforeReplace(content: Content) {
        if (ignoreModification) {
            return
        }
        replaceMark = true
        targetContent = content
    }

    override fun afterInsert(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
        if (ignoreModification) {
            return
        }
        val insAction = InsertAction()
        insAction.startLine = startLine
        insAction.startColumn = startColumn
        insAction.endLine = endLine
        insAction.endColumn = endColumn
        insAction.text = insertedContent
        val delAction = deleteAction
        if (replaceMark && delAction != null) {
            val rep = ReplaceAction()
            rep.delete = delAction
            rep.insert = insAction
            rep.cursor = memorizedCursorRange
            pushAction(content, rep)
        } else {
            insAction.cursor = memorizedCursorRange
            pushAction(content, insAction)
        }
        deleteAction = null
        insertAction = null
        replaceMark = false
    }

    override fun afterDelete(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
        if (ignoreModification) {
            return
        }
        val delAction = DeleteAction()
        delAction.endColumn = endColumn
        delAction.startColumn = startColumn
        delAction.endLine = endLine
        delAction.startLine = startLine
        delAction.text = deletedContent
        delAction.cursor = memorizedCursorRange
        deleteAction = delAction
        if (!replaceMark) {
            pushAction(content, delAction)
        }
    }

    override fun beforeModification(content: Content) {
        if (!_undoEnabled || !content.isCursorCreated || (replaceMark && deleteAction != null)) {
            return
        }
        val cursor = content.cursor
        memorizedCursorRange = cursor?.range
    }

    /**
     * Base class of content actions
     *
     * @author Rosemoe
     */
    abstract class ContentAction : Parcelable {

        @Transient
        @JvmField
        var cursor: TextRange? = null

        /**
         * Undo this action
         *
         * @param content On the given object
         */
        abstract fun undo(content: Content)

        /**
         * Redo this action
         *
         * @param content On the given object
         */
        abstract fun redo(content: Content)

        /**
         * Get whether the target action can be merged with this action
         *
         * @param action Target action to merge
         * @return Whether they can merge
         */
        abstract fun canMerge(action: ContentAction): Boolean

        /**
         * Merge with target action
         *
         * @param action Target action to merge
         */
        abstract fun merge(action: ContentAction)
    }

    /**
     * Insert action model for UndoManager
     *
     * @author Rosemoe
     */
    class InsertAction : ContentAction() {

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<InsertAction> = object : Parcelable.Creator<InsertAction> {
                override fun createFromParcel(parcel: Parcel): InsertAction {
                    val o = InsertAction()
                    o.startLine = parcel.readInt()
                    o.startColumn = parcel.readInt()
                    o.endLine = parcel.readInt()
                    o.endColumn = parcel.readInt()
                    o.text = parcel.readString()
                    return o
                }

                override fun newArray(size: Int): Array<InsertAction?> {
                    return arrayOfNulls(size)
                }
            }
        }

        @JvmField
        var startLine: Int = 0
        @JvmField
        var endLine: Int = 0
        @JvmField
        var startColumn: Int = 0
        @JvmField
        var endColumn: Int = 0
        @Transient
        @JvmField
        var createTime: Long = System.currentTimeMillis()
        @JvmField
        var text: CharSequence? = null

        override fun undo(content: Content) {
            content.delete(startLine, startColumn, endLine, endColumn)
        }

        override fun redo(content: Content) {
            val t = text ?: return
            content.insert(startLine, startColumn, t)
        }

        override fun canMerge(action: ContentAction): Boolean {
            if (action is InsertAction) {
                val acText = action.text
                val currentText = text
                if (acText != null && currentText != null) {
                    return (action.startColumn == endColumn && action.startLine == endLine
                            && acText.length + currentText.length < 10000
                            && abs(action.createTime - createTime) < sMergeTimeLimit)
                }
            }
            return false
        }

        override fun merge(action: ContentAction) {
            require(canMerge(action))
            val ac = action as InsertAction
            this.endColumn = ac.endColumn
            this.endLine = ac.endLine
            val currentText = text
            val sb = if (currentText is StringBuilder) {
                currentText
            } else {
                val newSb = StringBuilder(currentText ?: "")
                text = newSb
                newSb
            }
            sb.append(ac.text)
        }

        override fun toString(): String {
            return "InsertAction{startLine=$startLine, endLine=$endLine, startColumn=$startColumn, endColumn=$endColumn, createTime=$createTime, text=$text}"
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(startLine)
            parcel.writeInt(startColumn)
            parcel.writeInt(endLine)
            parcel.writeInt(endColumn)
            parcel.writeString(text?.toString())
        }
    }

    /**
     * MultiAction saves several actions for UndoManager
     *
     * @author Rose
     */
    class MultiAction : ContentAction() {

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<MultiAction> = object : Parcelable.Creator<MultiAction> {
                override fun createFromParcel(parcel: Parcel): MultiAction {
                    val o = MultiAction()
                    var count = parcel.readInt()
                    while (count > 0) {
                        val action: ContentAction? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            parcel.readParcelable(MultiAction::class.java.classLoader, ContentAction::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            parcel.readParcelable(MultiAction::class.java.classLoader)
                        }
                        if (action != null) {
                            o._actions.add(action)
                        }
                        count--
                    }
                    return o
                }

                override fun newArray(size: Int): Array<MultiAction?> {
                    return arrayOfNulls(size)
                }
            }
        }

        private val _actions: MutableList<ContentAction> = ArrayList()

        val actions: List<ContentAction>
            get() = _actions

        fun addAction(action: ContentAction) {
            if (_actions.isEmpty()) {
                _actions.add(action)
            } else {
                val last = _actions[_actions.size - 1]
                if (last.canMerge(action)) {
                    last.merge(action)
                } else {
                    _actions.add(action)
                }
            }
        }

        override fun undo(content: Content) {
            for (i in _actions.size - 1 downTo 0) {
                _actions[i].undo(content)
            }
        }

        override fun redo(content: Content) {
            for (i in _actions.indices) {
                _actions[i].redo(content)
            }
        }

        override fun canMerge(action: ContentAction): Boolean = false

        override fun merge(action: ContentAction) {
            throw UnsupportedOperationException()
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(_actions.size)
            for (action in _actions) {
                parcel.writeParcelable(action, flags)
            }
        }
    }

    /**
     * Delete action model for UndoManager
     *
     * @author Rose
     */
    class DeleteAction : ContentAction() {

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<DeleteAction> = object : Parcelable.Creator<DeleteAction> {
                override fun createFromParcel(parcel: Parcel): DeleteAction {
                    val o = DeleteAction()
                    o.startLine = parcel.readInt()
                    o.startColumn = parcel.readInt()
                    o.endLine = parcel.readInt()
                    o.endColumn = parcel.readInt()
                    o.text = parcel.readString()
                    return o
                }

                override fun newArray(size: Int): Array<DeleteAction?> {
                    return arrayOfNulls(size)
                }
            }
        }

        @JvmField
        var startLine: Int = 0
        @JvmField
        var endLine: Int = 0
        @JvmField
        var startColumn: Int = 0
        @JvmField
        var endColumn: Int = 0
        @Transient
        @JvmField
        var createTime: Long = System.currentTimeMillis()
        @JvmField
        var text: CharSequence? = null

        override fun undo(content: Content) {
            val t = text ?: return
            content.insert(startLine, startColumn, t)
        }

        override fun redo(content: Content) {
            content.delete(startLine, startColumn, endLine, endColumn)
        }

        override fun canMerge(action: ContentAction): Boolean {
            if (action is DeleteAction) {
                val acText = action.text
                val currentText = text
                if (acText != null && currentText != null) {
                    return (action.endColumn == startColumn && action.endLine == startLine
                            && acText.length + currentText.length < 10000
                            && abs(action.createTime - createTime) < sMergeTimeLimit)
                }
            }
            return false
        }

        override fun merge(action: ContentAction) {
            require(canMerge(action))
            val ac = action as DeleteAction
            this.startColumn = ac.startColumn
            this.startLine = ac.startLine
            val currentText = text
            val sb = if (currentText is StringBuilder) {
                currentText
            } else {
                val newSb = StringBuilder(currentText ?: "")
                text = newSb
                newSb
            }
            sb.insert(0, ac.text)
        }

        override fun toString(): String {
            return "DeleteAction{startLine=$startLine, endLine=$endLine, startColumn=$startColumn, endColumn=$endColumn, createTime=$createTime, text=$text}"
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(startLine)
            parcel.writeInt(startColumn)
            parcel.writeInt(endLine)
            parcel.writeInt(endColumn)
            parcel.writeString(text?.toString())
        }
    }

    /**
     * Replace action model for UndoManager
     *
     * @author Rose
     */
    class ReplaceAction : ContentAction() {

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<ReplaceAction> = object : Parcelable.Creator<ReplaceAction> {
                override fun createFromParcel(parcel: Parcel): ReplaceAction {
                    val o = ReplaceAction()
                    o.insert = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        parcel.readParcelable(ReplaceAction::class.java.classLoader, InsertAction::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        parcel.readParcelable(ReplaceAction::class.java.classLoader)
                    }
                    o.delete = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        parcel.readParcelable(ReplaceAction::class.java.classLoader, DeleteAction::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        parcel.readParcelable(ReplaceAction::class.java.classLoader)
                    }
                    return o
                }

                override fun newArray(size: Int): Array<ReplaceAction?> {
                    return arrayOfNulls(size)
                }
            }
        }

        @JvmField
        var insert: InsertAction? = null
        @JvmField
        var delete: DeleteAction? = null

        override fun undo(content: Content) {
            insert?.undo(content)
            delete?.undo(content)
        }

        override fun redo(content: Content) {
            delete?.redo(content)
            insert?.redo(content)
        }

        override fun canMerge(action: ContentAction): Boolean = false

        override fun merge(action: ContentAction) {
            throw UnsupportedOperationException()
        }

        override fun toString(): String {
            return "ReplaceAction{insert=$insert, delete=$delete}"
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(insert, flags)
            parcel.writeParcelable(delete, flags)
        }
    }
}
