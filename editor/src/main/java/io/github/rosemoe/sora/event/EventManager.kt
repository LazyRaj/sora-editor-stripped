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
package io.github.rosemoe.sora.event

import java.util.Arrays
import java.util.LinkedList
import java.util.Vector
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * This class manages event dispatching in editor.
 * Users can either register their event receivers here or dispatch event to
 * the receivers in this manager.
 *
 * There may be several EventManagers in one editor instance. For example, each plugin
 * will have its own EventManager and the editor also has a root event manager for external
 * listeners.
 *
 * Note that the event type must be exact. That's to say, you need to use a terminal class instead
 * of using its parent classes. For instance, if you register a receiver with the event type [Event],
 * no event will be sent to your receiver.
 *
 * @author Rosemoe
 */
class EventManager @JvmOverloads constructor(
    private val parent: EventManager? = null
) {

    private val receivers: MutableMap<Class<*>, Receivers<*>> = HashMap()
    private val lock = ReentrantReadWriteLock()
    private val children: MutableList<EventManager> = Vector()
    private val caches: Array<Array<EventReceiver<*>?>?> = arrayOfNulls(5)

    var isEnabled: Boolean = true
        set(value) {
            if (parent == null && !value) {
                throw IllegalStateException("The event manager is set to be root, and can not be disabled")
            }
            field = value
        }

    private var detached: Boolean = false

    init {
        parent?.children?.add(this)
    }

    /**
     * Get root node
     */
    val rootManager: EventManager
        get() {
            checkDetached()
            return parent?.rootManager ?: this
        }

    /**
     * Get root manager and dispatch the given event
     *
     * @see dispatchEvent
     */
    fun <T : Event> dispatchEventFromRoot(event: T): Int {
        return rootManager.dispatchEvent(event)
    }

    /**
     * Detached from parent.
     * This manager will not receive future events from parent
     */
    fun detach() {
        if (parent == null) {
            throw IllegalStateException("root manager can not be detached")
        }
        checkDetached()
        detached = true
        parent.children.remove(this)
    }

    private fun checkDetached() {
        if (detached) {
            throw IllegalStateException("already detached")
        }
    }

    /**
     * Get receivers container of a given event type safely
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Event> getReceivers(type: Class<T>): Receivers<T> {
        lock.read {
            val existing = receivers[type]
            if (existing != null) {
                return existing as Receivers<T>
            }
        }
        lock.write {
            var result = receivers[type]
            if (result == null) {
                result = Receivers<T>()
                receivers[type] = result
            }
            return result as Receivers<T>
        }
    }

    /**
     * @see subscribeEvent
     */
    fun <T : Event> subscribe(eventType: Class<T>, receiver: EventReceiver<T>): SubscriptionReceipt<T> {
        return subscribeEvent(eventType, receiver)
    }

    /**
     * Subscribe an event and never unsubscribe from event receiver
     */
    fun <T : Event> subscribeAlways(eventType: Class<T>, receiver: NoUnsubscribeReceiver<T>): SubscriptionReceipt<T> {
        return subscribeEvent(eventType) { event, _ -> receiver.onEvent(event) }
    }

    /**
     * Register a receiver of the given event.
     *
     * @param eventType Event type to be received
     * @param receiver  Receiver of event
     * @param T         Event type
     */
    fun <T : Event> subscribeEvent(eventType: Class<T>, receiver: EventReceiver<T>): SubscriptionReceipt<T> {
        val receiversList = getReceivers(eventType)
        receiversList.lock.write {
            val list = receiversList.receivers
            if (!list.contains(receiver)) {
                list.add(receiver)
            }
        }
        return SubscriptionReceipt(this, eventType, receiver)
    }

    /**
     * Dispatch the given event to its receivers registered in this manager.
     *
     * @param event Event to dispatch
     * @param T     Event type
     * @return The event's intercept targets
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Event> dispatchEvent(event: T): Int {
        if (!isEnabled) {
            return event.interceptTargets
        }
        val receiversContainer = getReceivers(event.javaClass)
        val receiverArr: Array<EventReceiver<T>?>
        val count: Int
        receiversContainer.lock.read {
            count = receiversContainer.receivers.size
            receiverArr = obtainBuffer(count)
            for (i in 0 until count) {
                receiverArr[i] = receiversContainer.receivers[i]
            }
        }
        var unsubscribedReceivers: MutableList<EventReceiver<T>>? = null
        try {
            val unsubscribe = Unsubscribe()
            var i = 0
            while (i < count && (event.interceptTargets and InterceptTarget.TARGET_RECEIVERS) == 0) {
                val receiver = receiverArr[i]
                if (receiver != null) {
                    receiver.onReceive(event, unsubscribe)
                    if (unsubscribe.isUnsubscribed) {
                        if (unsubscribedReceivers == null) {
                            unsubscribedReceivers = LinkedList()
                        }
                        unsubscribedReceivers.add(receiver)
                    }
                    unsubscribe.reset()
                }
                i++
            }
        } finally {
            if (unsubscribedReceivers != null) {
                receiversContainer.lock.write {
                    receiversContainer.receivers.removeAll(unsubscribedReceivers)
                }
            }
            recycleBuffer(receiverArr)
        }

        var i = 0
        while (i < children.size && (event.interceptTargets and InterceptTarget.TARGET_RECEIVERS) == 0) {
            val sub = try {
                children[i]
            } catch (e: IndexOutOfBoundsException) {
                null
            }
            sub?.dispatchEvent(event)
            i++
        }
        return event.interceptTargets
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : Event> obtainBuffer(size: Int): Array<EventReceiver<V>?> {
        var res: Array<EventReceiver<V>?>? = null
        synchronized(this) {
            for (i in caches.indices) {
                val cached = caches[i]
                if (cached != null && cached.size >= size) {
                    res = cached as Array<EventReceiver<V>?>
                    caches[i] = null
                    break
                }
            }
        }
        return res ?: arrayOfNulls(size)
    }

    private fun recycleBuffer(array: Array<*>?) {
        if (array == null) {
            return
        }
        synchronized(this) {
            for (i in caches.indices) {
                if (caches[i] == null) {
                    Arrays.fill(array, null)
                    @Suppress("UNCHECKED_CAST")
                    caches[i] = array as Array<EventReceiver<*>?>
                    break
                }
            }
        }
    }

    /**
     * Internal class for saving receivers of each type
     *
     * @param T Event type
     */
    internal class Receivers<T : Event> {
        val lock = ReentrantReadWriteLock()
        val receivers: MutableList<EventReceiver<T>> = ArrayList()
    }

    fun interface NoUnsubscribeReceiver<T : Event> {
        fun onEvent(event: T)
    }
}
