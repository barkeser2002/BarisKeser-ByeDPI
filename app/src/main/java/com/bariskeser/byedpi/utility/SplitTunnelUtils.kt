package com.bariskeser.byedpi.utility

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log

private const val TAG = "SplitTunnelUtils"

/** SharedPreferences key holding the set of package names that bypass the VPN. */
const val SPLIT_EXCLUDED_APPS_KEY = "split_excluded_apps"

/** A launchable app that can be excluded from the VPN. */
data class SplitAppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)

/** Returns a fresh copy of the excluded package set, safe to modify. */
fun SharedPreferences.getSplitExcludedApps(): Set<String> =
    getStringSet(SPLIT_EXCLUDED_APPS_KEY, null)?.toSet() ?: emptySet()

/** Stores a copy of [packages] so the caller's set is never shared with SharedPreferences. */
fun SharedPreferences.setSplitExcludedApps(packages: Set<String>) {
    edit().putStringSet(SPLIT_EXCLUDED_APPS_KEY, HashSet(packages)).apply()
}

/**
 * Lists apps that have a launcher icon, excluding [ownPackage].
 * Blocking: call from a background dispatcher. Requires a matching
 * `<queries>` entry in the manifest on API 30+.
 */
fun loadSplitLaunchableApps(packageManager: PackageManager, ownPackage: String): List<SplitAppEntry> {
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    return packageManager.queryIntentActivities(launcherIntent, 0)
        .asSequence()
        .filter { it.activityInfo != null && it.activityInfo.packageName != ownPackage }
        .distinctBy { it.activityInfo.packageName }
        .mapNotNull { info ->
            // One broken package must not hide the rest of the list.
            try {
                SplitAppEntry(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(packageManager)?.toString().orEmpty(),
                    icon = info.loadIcon(packageManager),
                )
            } catch (e: Exception) {
                Log.w(TAG, "Skipping unreadable package ${info.activityInfo.packageName}", e)
                null
            }
        }
        .sortedBy { it.label.lowercase() }
        .toList()
}
