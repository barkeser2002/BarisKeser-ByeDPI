package com.bariskeser.byedpi.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import com.bariskeser.byedpi.BuildConfig
import com.bariskeser.byedpi.R
import com.bariskeser.byedpi.data.Mode
import com.bariskeser.byedpi.services.ServiceManager
import com.bariskeser.byedpi.utility.*

class MainSettingsFragment : PreferenceFragmentCompat() {
    companion object {
        private val TAG: String = MainSettingsFragment::class.java.simpleName

        private const val SOURCE_URL = "https://github.com/barkeser2002/BarisKeser-ByeDPI"
        private const val PRIVACY_URL = "https://barkeser2002.github.io/BarisKeser-ByeDPI/"

        /**
         * The app is always dark. The [name] argument is ignored and kept only so
         * existing callers compile; this never throws for any input.
         */
        @Suppress("UNUSED_PARAMETER")
        fun setTheme(name: String) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            updatePreferences()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Declining in review mode withdraws consent, so a VPN that is running or still
        // connecting must stop too. The VPN service serializes the stop behind any start.
        parentFragmentManager.setFragmentResultListener(
            VpnDisclosureDialogFragment.REQUEST_KEY,
            this,
        ) { _, bundle ->
            if (!bundle.getBoolean(VpnDisclosureDialogFragment.RESULT_ACCEPTED) &&
                requireContext().getPreferences().mode() == Mode.VPN
            ) {
                ServiceManager.stopVpn(requireContext())
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_settings, rootKey)

        findPreferenceNotNull<Preference>("vpn_disclosure").setOnPreferenceClickListener {
            VpnDisclosureDialogFragment.newInstance(review = true)
                .show(parentFragmentManager, "vpn_disclosure_review")
            true
        }

        // Opened by the app, not by an intent in the XML: a missing browser must not crash.
        findPreferenceNotNull<Preference>("source_code").setOnPreferenceClickListener {
            openExternalUrl(SOURCE_URL)
            true
        }
        findPreferenceNotNull<Preference>("privacy_policy").setOnPreferenceClickListener {
            openExternalUrl(PRIVACY_URL)
            true
        }

        setEditTextPreferenceListener("dns_ip") {
            it.isBlank() || checkNotLocalIp(it)
        }

        val switchCommandLineSettings = findPreferenceNotNull<SwitchPreference>(
            "byedpi_enable_cmd_settings"
        )

        applyCommandLineMode(switchCommandLineSettings.isChecked)

        switchCommandLineSettings.setOnPreferenceChangeListener { _, newValue ->
            applyCommandLineMode(newValue as Boolean)
            true
        }

        val turkeyPreset = findPreferenceNotNull<DropDownPreference>("turkey_preset")
        turkeyPreset.setOnPreferenceChangeListener { _, newValue ->
            val cmd = newValue as String
            preferenceManager.sharedPreferences?.edit()
                ?.putString("byedpi_cmd_args", cmd)
                ?.putBoolean("byedpi_enable_cmd_settings", true)
                ?.apply()
            switchCommandLineSettings.isChecked = true
            applyCommandLineMode(true)
            true
        }

        findPreferenceNotNull<Preference>("version").summary = BuildConfig.VERSION_NAME

        updatePreferences()
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences?.registerOnSharedPreferenceChangeListener(preferenceListener)
        // Settings may have been changed by another screen (e.g. the strategy test)
        // while this fragment was paused, so re-read them from storage.
        syncCommandLineUi()
        updatePreferences()
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    private fun openExternalUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No app can open $url", e)
            Toast.makeText(requireContext(), R.string.no_browser_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyCommandLineMode(enable: Boolean) {
        findPreference<Preference>("byedpi_ui_settings")?.isEnabled = !enable
        findPreference<Preference>("byedpi_cmd_settings")?.isEnabled = enable
    }

    private fun syncCommandLineUi() {
        val prefs = preferenceManager?.sharedPreferences ?: return
        val enabled = prefs.getBoolean("byedpi_enable_cmd_settings", false)
        findPreference<SwitchPreference>("byedpi_enable_cmd_settings")?.isChecked = enabled
        applyCommandLineMode(enabled)

        val storedArgs = prefs.getString("byedpi_cmd_args", null)
        findPreference<DropDownPreference>("turkey_preset")?.let { preset ->
            if (storedArgs != null && preset.entryValues?.contains(storedArgs) == true) {
                preset.value = storedArgs
            }
        }
    }

    private fun updatePreferences() {
        val mode = findPreferenceNotNull<ListPreference>("byedpi_mode")
            .value.let { Mode.fromString(it) }
        val dns = findPreferenceNotNull<EditTextPreference>("dns_ip")
        val ipv6 = findPreferenceNotNull<SwitchPreference>("ipv6_enable")

        when (mode) {
            Mode.VPN -> {
                dns.isVisible = true
                ipv6.isVisible = true
            }

            Mode.Proxy -> {
                dns.isVisible = false
                ipv6.isVisible = false
            }
        }
    }
}
