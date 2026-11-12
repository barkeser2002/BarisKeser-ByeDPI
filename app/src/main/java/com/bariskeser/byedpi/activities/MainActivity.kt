package com.bariskeser.byedpi.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bariskeser.byedpi.BuildConfig
import com.bariskeser.byedpi.R
import com.bariskeser.byedpi.data.*
import com.bariskeser.byedpi.databinding.ActivityMainBinding
import com.bariskeser.byedpi.fragments.VpnDisclosureDialogFragment
import com.bariskeser.byedpi.services.ServiceManager
import com.bariskeser.byedpi.services.appStatus
import com.bariskeser.byedpi.utility.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName

        private const val PRIVACY_URL = "https://barkeser2002.github.io/BarisKeser-ByeDPI/"
        private const val DISCLOSURE_TAG = "vpn_disclosure"

        private fun collectLogs(): String? =
            try {
                Runtime.getRuntime()
                    .exec("logcat *:D -d")
                    .inputStream.bufferedReader()
                    .use { it.readText() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to collect logs", e)
                null
            }
    }

    private val vpnRegister =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                startTunnel(Mode.VPN)
            } else {
                Toast.makeText(this, R.string.vpn_permission_denied, Toast.LENGTH_SHORT).show()
                updateStatus()
            }
        }

    /**
     * The notification permission is requested in context, right before the tunnel
     * starts. The tunnel starts whether or not the user grants it.
     */
    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            launchService()
        }

    private val logsRegister =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val logs = collectLogs()

                if (logs == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.logs_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val uri = it.data?.data ?: run {
                        Log.e(TAG, "No data in result")
                        return@launch
                    }
                    contentResolver.openOutputStream(uri)?.use {
                        try {
                            it.write(logs.toByteArray())
                        } catch (e: IOException) {
                            Log.e(TAG, "Failed to save logs", e)
                        }
                    } ?: run {
                        Log.e(TAG, "Failed to open output stream")
                    }
                }
            }
        }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Received intent: ${intent?.action}")

            if (intent == null) {
                Log.w(TAG, "Received null intent")
                return
            }

            val senderOrd = intent.getIntExtra(SENDER, -1)
            val sender = Sender.entries.getOrNull(senderOrd)
            if (sender == null) {
                Log.w(TAG, "Received intent with unknown sender: $senderOrd")
                return
            }

            when (val action = intent.action) {
                STARTED_BROADCAST,
                STOPPED_BROADCAST -> updateStatus()

                FAILED_BROADCAST -> {
                    Toast.makeText(
                        context,
                        getString(R.string.failed_to_start, sender.name),
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateStatus()
                }

                else -> Log.w(TAG, "Unknown action: $action")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always dark; set before the activity is created so it takes effect.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Registered on every creation so the result still arrives after a rotation.
        supportFragmentManager.setFragmentResultListener(
            VpnDisclosureDialogFragment.REQUEST_KEY,
            this,
        ) { _, bundle ->
            if (bundle.getBoolean(VpnDisclosureDialogFragment.RESULT_ACCEPTED)) {
                proceedStart()
            } else {
                Toast.makeText(this, R.string.vpn_disclosure_declined_hint, Toast.LENGTH_LONG)
                    .show()
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(STARTED_BROADCAST)
            addAction(STOPPED_BROADCAST)
            addAction(FAILED_BROADCAST)
        }

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, intentFilter)
        }

        binding.statusButton.setOnClickListener {
            val (status, _) = appStatus
            when (status) {
                AppStatus.Halted -> start()
                AppStatus.Running -> stop()
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { showSection(true); true }
                R.id.nav_about -> { showSection(false); true }
                R.id.nav_settings -> { openSettings(); false }
                else -> false
            }
        }
        // The selection is restored after onCreate, so read it once the view is laid out.
        binding.bottomNav.post {
            showSection(binding.bottomNav.selectedItemId != R.id.nav_about)
        }

        binding.aboutVersion.text = BuildConfig.VERSION_NAME
        binding.aboutSource.setOnClickListener {
            openUrl("https://github.com/barkeser2002/BarisKeser-ByeDPI")
        }
        binding.aboutDocs.setOnClickListener {
            openUrl(getString(R.string.byedpi_docs))
        }
        binding.aboutPrivacy.setOnClickListener {
            openUrl(PRIVACY_URL)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val (status, _) = appStatus

        return when (item.itemId) {
            R.id.action_settings -> {
                if (status == AppStatus.Halted) {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_SHORT)
                        .show()
                }
                true
            }

            R.id.action_save_logs -> {
                val intent =
                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, "byedpi.log")
                    }

                logsRegister.launch(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Entry point of the Connect button. VPN mode needs the in-app disclosure to be
     * accepted first. Proxy mode starts directly.
     */
    private fun start() {
        if (getPreferences().mode() == Mode.VPN && !hasVpnConsent()) {
            showVpnDisclosure()
            return
        }
        proceedStart()
    }

    private fun showVpnDisclosure() {
        if (supportFragmentManager.findFragmentByTag(DISCLOSURE_TAG) != null) return
        VpnDisclosureDialogFragment.newInstance(review = false)
            .show(supportFragmentManager, DISCLOSURE_TAG)
    }

    /**
     * Asks for the notification permission in context (Android 13+), then starts the
     * tunnel. Rationale is shown only when the system asks for it.
     */
    private fun proceedStart() {
        if (!needsNotificationPermission()) {
            launchService()
            return
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.notif_rationale_title)
                .setMessage(R.string.notif_rationale_text)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> launchService() }
                .setOnCancelListener { launchService() }
                .show()
        } else {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED

    private fun launchService() {
        when (getPreferences().mode()) {
            Mode.VPN -> {
                val intentPrepare = VpnService.prepare(this)
                if (intentPrepare != null) {
                    vpnRegister.launch(intentPrepare)
                } else {
                    startTunnel(Mode.VPN)
                }
            }

            Mode.Proxy -> startTunnel(Mode.Proxy)
        }
    }

    private fun startTunnel(mode: Mode) {
        ServiceManager.start(this, mode)
        maybePromptBatteryOptimization()
    }

    /**
     * Offers, once, to open the system battery-optimization list. Exempting the app
     * is optional. It only helps on phones whose manufacturer restricts background
     * apps aggressively, so the prompt is not needed on most devices. If the user
     * declines we remember that and don't ask again.
     */
    private fun maybePromptBatteryOptimization() {
        val preferences = getPreferences()
        if (preferences.getBoolean("battery_opt_prompted", false)) return
        if (isIgnoringBatteryOptimizations(this)) return

        AlertDialog.Builder(this)
            .setTitle(R.string.battery_opt_title)
            .setMessage(R.string.battery_opt_message)
            .setPositiveButton(R.string.battery_opt_allow) { _, _ ->
                preferences.edit().putBoolean("battery_opt_prompted", true).apply()
                openBatteryOptimizationSettings(this)
            }
            .setNegativeButton(R.string.battery_opt_later) { _, _ ->
                preferences.edit().putBoolean("battery_opt_prompted", true).apply()
            }
            .show()
    }

    private fun stop() {
        ServiceManager.stop(this)
    }

    private fun updateStatus() {
        val (status, mode) = appStatus

        Log.i(TAG, "Updating status: $status, $mode")

        val preferences = getPreferences()
        val proxyIp = preferences.getStringNotNull("byedpi_proxy_ip", "127.0.0.1")
        val proxyPort = preferences.getStringNotNull("byedpi_proxy_port", "1080")
        binding.proxyAddress.text = getString(R.string.proxy_address, proxyIp, proxyPort)

        binding.infoMode.text = getString(R.string.mode_setting) + ": " +
                if (preferences.mode() == Mode.VPN) "VPN" else "Proxy"
        binding.infoDns.text = getString(R.string.dbs_ip_setting) + ": " +
                preferences.getStringNotNull("dns_ip", "1.1.1.1")

        when (status) {
            AppStatus.Halted -> {
                when (preferences.mode()) {
                    Mode.VPN -> {
                        binding.statusText.setText(R.string.vpn_disconnected)
                        binding.statusButton.setText(R.string.vpn_connect)
                    }

                    Mode.Proxy -> {
                        binding.statusText.setText(R.string.proxy_down)
                        binding.statusButton.setText(R.string.proxy_start)
                    }
                }
                binding.statusButton.isEnabled = true
                setStateColors(connected = false)
            }

            AppStatus.Running -> {
                when (mode) {
                    Mode.VPN -> {
                        binding.statusText.setText(R.string.vpn_connected)
                        binding.statusButton.setText(R.string.vpn_disconnect)
                    }

                    Mode.Proxy -> {
                        binding.statusText.setText(R.string.proxy_up)
                        binding.statusButton.setText(R.string.proxy_stop)
                    }
                }
                binding.statusButton.isEnabled = true
                setStateColors(connected = true)
            }
        }
    }

    /**
     * Disconnected: red button with white text, grey ring and dot.
     * Connected: dark button with soft red text, green ring and dot.
     */
    private fun setStateColors(connected: Boolean) {
        val buttonColor: Int
        val textColor: Int
        val accentColor: Int
        if (connected) {
            buttonColor = R.color.th_connected_button
            textColor = R.color.th_red_soft
            accentColor = R.color.th_green
        } else {
            buttonColor = R.color.th_red
            textColor = R.color.white
            accentColor = R.color.th_text3
        }

        binding.statusButton.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, buttonColor))
        binding.statusButton.setTextColor(ContextCompat.getColor(this, textColor))
        binding.statusRing.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, accentColor))
        binding.statusDot.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, accentColor))
    }

    private fun showSection(home: Boolean) {
        binding.homeView.visibility = if (home) View.VISIBLE else View.GONE
        binding.aboutView.visibility = if (home) View.GONE else View.VISIBLE
    }

    private fun openSettings() {
        val (status, _) = appStatus
        if (status == AppStatus.Halted) {
            startActivity(Intent(this, SettingsActivity::class.java))
        } else {
            Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open url", e)
        }
    }
}