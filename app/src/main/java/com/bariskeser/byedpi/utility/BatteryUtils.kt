package com.bariskeser.byedpi.utility

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

private const val TAG = "BatteryUtils"

/**
 * Whether the system already exempts the app from battery optimization. The
 * exemption is optional: most devices keep the tunnel running without it, and
 * only some manufacturers' background restrictions need the user to change it.
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

/**
 * Opens the system battery-optimization list so the user can pick "Don't optimize"
 * for this app. The app does not request the exemption directly, so it needs no
 * special permission and does not use the direct exemption dialog.
 * Falls back to the app details screen if the list is not available.
 */
fun openBatteryOptimizationSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "Battery optimization list not available, opening app details", e)
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                )
            )
        } catch (e2: ActivityNotFoundException) {
            Log.e(TAG, "Could not open battery settings", e2)
        }
    }
}
