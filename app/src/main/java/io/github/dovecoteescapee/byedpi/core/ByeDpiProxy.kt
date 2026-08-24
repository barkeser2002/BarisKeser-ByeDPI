package io.github.dovecoteescapee.byedpi.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ByeDpiProxy {
    companion object {
        init {
            System.loadLibrary("byedpi")
        }
    }

    private val mutex = Mutex()

    suspend fun startProxy(preferences: ByeDpiProxyPreferences): Int =
        jniStartProxy(preferences.args)

    suspend fun stopProxy(): Int = mutex.withLock { jniStopProxy() }

    fun forceClose(): Int = jniForceClose()

    private external fun jniStartProxy(args: Array<String>): Int
    private external fun jniStopProxy(): Int
    private external fun jniForceClose(): Int
}
