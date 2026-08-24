package com.bariskeser.byedpi.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.bariskeser.byedpi.R

class ByeDpiCommandLineSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.byedpi_cmd_settings, rootKey)
    }
}
