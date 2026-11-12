package com.bariskeser.byedpi.utility

import android.content.Context
import android.content.SharedPreferences

private const val VPN_CONSENT_PREFS = "vpn_consent"
private const val VPN_CONSENT_KEY = "vpn_disclosure_accepted_v1"

/**
 * Consent is stored in its own preferences file, which the backup rules exclude.
 * A restored device therefore shows the disclosure again before the VPN can start.
 */
private fun Context.vpnConsentPreferences(): SharedPreferences =
    getSharedPreferences(VPN_CONSENT_PREFS, Context.MODE_PRIVATE)

/**
 * Whether the user has accepted the in-app VPN disclosure. VPN mode must not
 * start, and the system VPN consent must not be requested, until this is true.
 */
fun Context.hasVpnConsent(): Boolean =
    vpnConsentPreferences().getBoolean(VPN_CONSENT_KEY, false)

fun Context.setVpnConsent(accepted: Boolean) {
    vpnConsentPreferences().edit().putBoolean(VPN_CONSENT_KEY, accepted).apply()
}
