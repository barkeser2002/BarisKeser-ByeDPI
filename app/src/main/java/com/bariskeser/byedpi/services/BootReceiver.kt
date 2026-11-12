package com.bariskeser.byedpi.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.bariskeser.byedpi.data.Mode
import com.bariskeser.byedpi.utility.getPreferences
import com.bariskeser.byedpi.utility.hasVpnConsent
import com.bariskeser.byedpi.utility.mode

/**
 * Starts the tunnel after the device boots when the user enabled
 * "auto_connect_boot". The mode is the one selected in settings.
 *
 * VPN mode is only started when the VPN permission was already granted,
 * because the consent dialog cannot be shown from a background receiver.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_QUICKBOOT_POWERON) {
            return
        }

        val prefs = context.getPreferences()
        if (!prefs.getBoolean(PREF_AUTO_CONNECT_BOOT, false)) {
            Log.i(TAG, "Auto-connect on boot is disabled, skipping")
            return
        }

        try {
            when (prefs.mode()) {
                Mode.VPN -> {
                    if (context.hasVpnConsent() && VpnService.prepare(context) == null) {
                        ServiceManager.start(context, Mode.VPN)
                    } else {
                        Log.w(
                            TAG,
                            "VPN disclosure not accepted or VPN permission not granted, skipping auto-connect on boot"
                        )
                    }
                }

                Mode.Proxy -> ServiceManager.start(context, Mode.Proxy)
            }
        } catch (e: Exception) {
            // Android 12+ can refuse a foreground service start from the background.
            Log.e(TAG, "Auto-connect on boot failed", e)
        }
    }

    companion object {
        private val TAG: String = BootReceiver::class.java.simpleName

        const val PREF_AUTO_CONNECT_BOOT = "auto_connect_boot"

        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
