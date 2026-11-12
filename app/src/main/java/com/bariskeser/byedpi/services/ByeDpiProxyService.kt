package com.bariskeser.byedpi.services

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.bariskeser.byedpi.R
import com.bariskeser.byedpi.core.ByeDpiProxy
import com.bariskeser.byedpi.core.ByeDpiProxyPreferences
import com.bariskeser.byedpi.data.*
import com.bariskeser.byedpi.utility.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ByeDpiProxyService : LifecycleService() {
    private var proxy = ByeDpiProxy()
    private var proxyJob: Job? = null
    private val mutex = Mutex()

    companion object {
        private val TAG: String = ByeDpiProxyService::class.java.simpleName
        private const val FOREGROUND_SERVICE_ID: Int = 2
        private const val NOTIFICATION_CHANNEL_ID: String = "ByeDPI Proxy"

        private var status: ServiceStatus = ServiceStatus.Disconnected
    }

    override fun onCreate() {
        super.onCreate()
        registerNotificationChannel(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.proxy_channel_name,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return when (val action = intent?.action) {
            START_ACTION, null -> {
                // Must be called for every startForegroundService() delivery (API 26+).
                startForegroundCompat()
                lifecycleScope.launch { start() }
                START_STICKY
            }

            STOP_ACTION -> {
                startForegroundCompat()
                lifecycleScope.launch { stop() }
                START_NOT_STICKY
            }

            else -> {
                Log.w(TAG, "Unknown action: $action")
                stopSelf(startId)
                START_NOT_STICKY
            }
        }
    }

    private suspend fun start() {
        Log.i(TAG, "Starting")

        if (status == ServiceStatus.Connected) {
            Log.w(TAG, "Proxy already connected")
            return
        }

        try {
            mutex.withLock {
                startProxy()
            }
            updateStatus(ServiceStatus.Connected)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start proxy", e)
            updateStatus(ServiceStatus.Failed)
            stop()
        }
    }

    private fun startForegroundCompat() {
        ServiceCompat.startForeground(
            this,
            FOREGROUND_SERVICE_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    private suspend fun stop() {
        Log.i(TAG, "Stopping VPN")

        mutex.withLock {
            stopProxy()
        }
        updateStatus(ServiceStatus.Disconnected)
        stopSelf()
    }

    private suspend fun startProxy() {
        Log.i(TAG, "Starting proxy")

        if (proxyJob != null) {
            Log.w(TAG, "Proxy fields not null")
            throw IllegalStateException("Proxy fields not null")
        }

        proxy = ByeDpiProxy()
        val preferences = getByeDpiPreferences()

        proxyJob = lifecycleScope.launch(Dispatchers.IO) {
            val code = proxy.startProxy(preferences)

            withContext(Dispatchers.Main) {
                if (code != 0) {
                    Log.e(TAG, "Proxy stopped with code $code")
                    updateStatus(ServiceStatus.Failed)
                } else {
                    updateStatus(ServiceStatus.Disconnected)
                }
                // The proxy has exited on its own. Stop the service so the foreground
                // notification does not stay up for a proxy that is no longer running.
                stopSelf()
            }
        }

        Log.i(TAG, "Proxy started")
    }

    private suspend fun stopProxy() {
        Log.i(TAG, "Stopping proxy")

        // No job means the proxy was never started or has already exited. Do not touch
        // the native state then: forceClose would act on a stale descriptor.
        val job = proxyJob
        if (job == null || status == ServiceStatus.Disconnected) {
            Log.w(TAG, "Proxy not running, nothing to stop")
            proxyJob = null
            return
        }

        proxy.stopProxy()
        val joined = withTimeoutOrNull(3000) { job.join() }
        if (joined == null) {
            Log.w(TAG, "join timeout, force closing")
            proxy.forceClose()
            job.join()
        }
        proxyJob = null

        Log.i(TAG, "Proxy stopped")
    }

    private fun getByeDpiPreferences(): ByeDpiProxyPreferences =
        ByeDpiProxyPreferences.fromSharedPreferences(getPreferences())

    private fun updateStatus(newStatus: ServiceStatus) {
        Log.d(TAG, "Proxy status changed from $status to $newStatus")

        status = newStatus

        setStatus(
            when (newStatus) {
                ServiceStatus.Connected -> AppStatus.Running
                ServiceStatus.Disconnected,
                ServiceStatus.Failed -> {
                    proxyJob = null
                    AppStatus.Halted
                }
            },
            Mode.Proxy
        )

        val intent = Intent(
            when (newStatus) {
                ServiceStatus.Connected -> STARTED_BROADCAST
                ServiceStatus.Disconnected -> STOPPED_BROADCAST
                ServiceStatus.Failed -> FAILED_BROADCAST
            }
        )
        intent.putExtra(SENDER, Sender.Proxy.ordinal)
        sendBroadcast(intent)
    }

    private fun createNotification(): Notification =
        createConnectionNotification(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.notification_title,
            R.string.proxy_notification_content,
            ByeDpiProxyService::class.java,
        )
}
