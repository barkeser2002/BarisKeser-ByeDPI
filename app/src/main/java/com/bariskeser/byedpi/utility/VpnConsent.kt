package com.bariskeser.byedpi.utility

import android.content.Context

private const val VPN_CONSENT_KEY = "vpn_disclosure_accepted_v1"

/**
 * Whether the user has accepted the in-app VPN disclosure. VPN mode must not
 * start, and the system VPN consent must not be requested, until this is true.
 */
fun Context.hasVpnConsent(): Boolean =
    getPreferences().getBoolean(VPN_CONSENT_KEY, false)

fun Context.setVpnConsent(accepted: Boolean) {
    getPreferences().edit().putBoolean(VPN_CONSENT_KEY, accepted).apply()
}
