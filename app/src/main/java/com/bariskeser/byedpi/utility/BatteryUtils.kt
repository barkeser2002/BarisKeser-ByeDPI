package com.bariskeser.byedpi.utility

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

private const val TAG = "BatteryUtils"

/**
 * Whether the app is exempt from Doze / battery optimization. When it is not,
 * the system (and aggressive OEM task-killers) can freeze or kill the foreground
 * service once the app leaves the foreground, which is the usual cause of the
 * tunnel dropping "by itself" in the background.
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

/**
 * Ask the system to exempt the app from battery optimization. Shows the system
 * grant dialog directly. Falls back to the per-app battery settings screen if the
 * direct request is unavailable on the device.
 */
@SuppressLint("BatteryLife")
fun requestIgnoreBatteryOptimizations(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w(TAG, "Direct battery-optimization request failed, opening settings", e)
        try {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (e2: Exception) {
            Log.e(TAG, "Could not open battery optimization settings", e2)
        }
    }
}
