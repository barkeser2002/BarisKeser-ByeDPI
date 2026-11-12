package com.bariskeser.byedpi.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.bariskeser.byedpi.core.StrategyTestGate
import com.bariskeser.byedpi.data.Mode
import com.bariskeser.byedpi.data.START_ACTION
import com.bariskeser.byedpi.data.STOP_ACTION
import com.bariskeser.byedpi.utility.hasVpnConsent

object ServiceManager {
    private val TAG: String = ServiceManager::class.java.simpleName

    /**
     * Starts the VPN or proxy service. Returns false when the start was refused:
     * the VPN disclosure is not accepted, or a strategy test is using the native proxy.
     */
    fun start(context: Context, mode: Mode): Boolean {
        if (StrategyTestGate.isActive) {
            Log.w(TAG, "Strategy test is running, not starting $mode")
            return false
        }

        when (mode) {
            Mode.VPN -> {
                if (!context.hasVpnConsent()) {
                    Log.w(TAG, "VPN disclosure not accepted, not starting VPN")
                    return false
                }
                Log.i(TAG, "Starting VPN")
                val intent = Intent(context, ByeDpiVpnService::class.java)
                intent.action = START_ACTION
                ContextCompat.startForegroundService(context, intent)
            }

            Mode.Proxy -> {
                Log.i(TAG, "Starting proxy")
                val intent = Intent(context, ByeDpiProxyService::class.java)
                intent.action = START_ACTION
                ContextCompat.startForegroundService(context, intent)
            }
        }
        return true
    }

    fun stop(context: Context) {
        val (_, mode) = appStatus
        when (mode) {
            Mode.VPN -> stopVpn(context)

            Mode.Proxy -> {
                Log.i(TAG, "Stopping proxy")
                val intent = Intent(context, ByeDpiProxyService::class.java)
                intent.action = STOP_ACTION
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }

    /**
     * Sends the stop to the VPN service regardless of the last reported status. A VPN
     * that is still connecting reports Halted, and the service serializes this stop
     * behind the start, so the tunnel is torn down once it has come up.
     */
    fun stopVpn(context: Context) {
        Log.i(TAG, "Stopping VPN")
        val intent = Intent(context, ByeDpiVpnService::class.java)
        intent.action = STOP_ACTION
        ContextCompat.startForegroundService(context, intent)
    }
}
