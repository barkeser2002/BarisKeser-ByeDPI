package com.bariskeser.byedpi.core

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-wide marker for a running strategy test. The test and the VPN or proxy
 * services share one native proxy state, so only one of them may run at a time.
 * The services check [isActive] before they start.
 */
object StrategyTestGate {
    private val active = AtomicBoolean(false)

    val isActive: Boolean
        get() = active.get()

    /** Returns true if the caller now holds the gate. */
    fun tryAcquire(): Boolean = active.compareAndSet(false, true)

    fun release() {
        active.set(false)
    }
}
