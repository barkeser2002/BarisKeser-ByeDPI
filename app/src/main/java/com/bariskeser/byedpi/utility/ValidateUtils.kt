package com.bariskeser.byedpi.utility

import android.net.InetAddresses
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.bariskeser.byedpi.R

private const val TAG = "ValidateUtils"

private val IPV4_REGEX = Regex("""\d{1,3}(\.\d{1,3}){3}""")
private val IPV6_GROUP_REGEX = Regex("""[0-9A-Fa-f]{1,4}""")
private val LOCAL_IPV6_LITERALS = setOf("::", "::1", "0:0:0:0:0:0:0:0", "0:0:0:0:0:0:0:1")

// Strict literal checks for API < 29 (InetAddresses is unavailable there).
// No DNS lookup is performed.
private fun isIpv4Literal(s: String): Boolean =
    IPV4_REGEX.matches(s) && s.split('.').all { it.toInt() <= 255 }

private fun isIpv6Literal(s: String): Boolean {
    if (s.count { it == ':' } < 2 || s.indexOf("::") != s.lastIndexOf("::")) return false
    if (s.startsWith(":") && !s.startsWith("::")) return false
    if (s.endsWith(":") && !s.endsWith("::")) return false
    val groups = s.split(':')
    return groups.withIndex().all { (i, g) ->
        g.isEmpty() || IPV6_GROUP_REGEX.matches(g) || (i == groups.lastIndex && isIpv4Literal(g))
    }
}

private fun isNumericIpLegacy(s: String): Boolean = isIpv4Literal(s) || isIpv6Literal(s)

private fun isLocalIpLegacy(s: String): Boolean =
    if (isIpv4Literal(s)) {
        val first = s.substringBefore('.').toInt()
        first == 0 || first == 127
    } else {
        s.lowercase() in LOCAL_IPV6_LITERALS
    }

fun checkIp(ip: String): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        InetAddresses.isNumericAddress(ip)
    } else {
        isNumericIpLegacy(ip)
    }

fun checkNotLocalIp(ip: String): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        InetAddresses.isNumericAddress(ip) && InetAddresses.parseNumericAddress(ip).let {
            !it.isAnyLocalAddress && !it.isLoopbackAddress
        }
    } else {
        isNumericIpLegacy(ip) && !isLocalIpLegacy(ip)
    }

fun PreferenceFragmentCompat.setEditTestPreferenceListenerPort(key: String) {
    setEditTestPreferenceListenerInt(key, 1, 65535)
}

fun PreferenceFragmentCompat.setEditTestPreferenceListenerInt(
    key: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE
) {
    setEditTextPreferenceListener(key) { value ->
        value.toIntOrNull()?.let { it in min..max } ?: false
    }
}

fun PreferenceFragmentCompat.setEditTextPreferenceListener(
    key: String,
    check: (String) -> Boolean
) {
    findPreferenceNotNull<EditTextPreference>(key)
        .setOnPreferenceChangeListener { preference, newValue ->
            when (newValue) {
                is String -> {
                    val valid = check(newValue)
                    if (!valid) {
                        Toast.makeText(
                            requireContext(),
                            requireContext().getString(R.string.invalid_value, preference.title, newValue),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    valid
                }

                else -> {
                    Log.w(
                        TAG,
                        "Invalid type for ${preference.key}: " +
                                "$newValue has type ${newValue::class.java}"
                    )
                    false
                }
            }
        }
}
